/*
 * v4l.c
 *
 * Copyright (C) 2001 Rasca, Berlin
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/types.h>
#include <sys/ioctl.h>
#include <sys/mman.h>
#include <fcntl.h>
#include <unistd.h>
#include <linux/types.h>
#include <linux/videodev.h>

#include "v4l.h"

#define min(a,b) ((a) < (b) ? (a) : (b))
#define max(a,b) ((a) > (b) ? (a) : (b))

/*
 * set the input and norm for the video4linux device
 */
int
v4l_set_input (int fd, int input, int norm)
{
  struct video_channel vid_chnl;

  if (input != INPUT_DEFAULT || norm != NORM_DEFAULT) {
    if (vid_chnl.channel != INPUT_DEFAULT)
      vid_chnl.channel = input;
    else
      vid_chnl.channel = 0;
    vid_chnl.norm = (unsigned short) -1;
    if (ioctl (fd, VIDIOCGCHAN, &vid_chnl) == -1) {
      perror ("ioctl (VIDIOCGCHAN)");
      return -1;
    } else {
      if (input != 0)
	vid_chnl.channel = input;
      if (norm != NORM_DEFAULT)
	vid_chnl.norm    = norm;
      if (ioctl (fd, VIDIOCSCHAN, &vid_chnl) == -1) {
	perror ("ioctl (VIDIOCSCHAN)");
	return -1;
      }
    }
  }
  return 0;
}

/*
 * check the size and readjust if necessary
 */
int
v4l_check_size (int fd, int *width, int *height)
{
  struct video_capability vid_caps;

  if (ioctl (fd, VIDIOCGCAP, &vid_caps) == -1) {
    perror ("ioctl (VIDIOCGCAP)");
    return -1;
  }
  /* readjust if necessary */
  if (*width > vid_caps.maxwidth || *width < vid_caps.minwidth) {
    *width = min (*width, vid_caps.maxwidth);
    *width = max (*width, vid_caps.minwidth);
    fprintf (stderr, "readjusting width to %d\n", *width);
  }
  if (*height > vid_caps.maxheight || *height < vid_caps.minheight) {
    *height = min (*height, vid_caps.maxheight);
    *height = max (*height, vid_caps.minheight);
    fprintf (stderr, "readjusting height to %d\n", *height);
  }
  return 0;
}

/*
 * check the requested palette and adjust if possible
 * seems not to work :-(
 */
int
v4l_check_palette (int fd, int *palette)
{
  struct video_picture vid_pic;

  if (!palette)
    return -1;

  if (ioctl (fd, VIDIOCGPICT, &vid_pic) == -1) {
    perror ("ioctl (VIDIOCGPICT)");
    return -1;
  }
  vid_pic.palette = *palette;
  if (ioctl (fd, VIDIOCSPICT, &vid_pic) == -1) {
    /* try YUV420P
     */
    fprintf (stderr, "failed\n");
    vid_pic.palette = *palette = VIDEO_PALETTE_YUV420P;
    if (ioctl (fd, VIDIOCSPICT, &vid_pic) == -1) {
      perror ("ioctl (VIDIOCSPICT) to YUV");
      /* ok, try grayscale..
       */
      vid_pic.palette = *palette = VIDEO_PALETTE_GREY;
      if (ioctl (fd, VIDIOCSPICT, &vid_pic) == -1) {
	perror ("ioctl (VIDIOCSPICT) to GREY");
	return -1;
      }
    }
  }
  return 0;
}

/*
 * check if driver supports mmap'ed buffer
 */
int
v4l_check_mmap (int fd, int *size)
{
  struct video_mbuf vid_buf;

  if (ioctl (fd, VIDIOCGMBUF, &vid_buf) == -1) {
    return -1;
  }
  if (size)
    *size = vid_buf.size;
  return 0;
}

/*
 * mute sound if available
 */
int
v4l_mute_sound (int fd)
{
  struct video_capability vid_caps;
  struct video_audio vid_aud;

  if (ioctl (fd, VIDIOCGCAP, &vid_caps) == -1) {
    perror ("ioctl (VIDIOCGCAP)");
    return -1;
  }
  if (vid_caps.audios > 0) {
    /* mute the sound */
    if (ioctl (fd, VIDIOCGAUDIO, &vid_aud) == -1) {
      return -1;
    } else {
      vid_aud.flags = VIDEO_AUDIO_MUTE;
      if (ioctl (fd, VIDIOCSAUDIO, &vid_aud) == -1)
	return -1;
    }
  }
  return 0;
}

/*
 * Turn a YUV4:2:0 block into an RGB block
 *
 * Video4Linux seems to use the blue, green, red channel
 * order convention-- rgb[0] is blue, rgb[1] is green, rgb[2] is red.
 *
 * Color space conversion coefficients taken from the excellent
 * http://www.inforamp.net/~poynton/ColorFAQ.html
 * In his terminology, this is a CCIR 601.1 YCbCr -> RGB.
 * Y values are given for all 4 pixels, but the U (Pb)
 * and V (Pr) are assumed constant over the 2x2 block.
 *
 * To avoid floating point arithmetic, the color conversion
 * coefficients are scaled into 16.16 fixed-point integers.
 * They were determined as follows:
 *
 *	double brightness = 1.0;  (0->black; 1->full scale) 
 *	double saturation = 1.0;  (0->greyscale; 1->full color)
 *	double fixScale = brightness * 256 * 256;
 *	int rvScale = (int)(1.402 * saturation * fixScale);
 *	int guScale = (int)(-0.344136 * saturation * fixScale);
 *	int gvScale = (int)(-0.714136 * saturation * fixScale);
 *	int buScale = (int)(1.772 * saturation * fixScale);
 *	int yScale = (int)(fixScale);	
 */

/* LIMIT: convert a 16.16 fixed-point value to a byte, with clipping. */
#define LIMIT(x) ((x)>0xffffff?0xff: ((x)<=0xffff?0:((x)>>16)))

/*
 */
static inline void
v4l_copy_420_block (int yTL, int yTR, int yBL, int yBR, int u, int v, 
		    int rowPixels, unsigned char * rgb, int bits)
{
  const int rvScale = 91881;
  const int guScale = -22553;
  const int gvScale = -46801;
  const int buScale = 116129;
  const int yScale  = 65536;
  int r, g, b;

  g = guScale * u + gvScale * v;
  r = rvScale * v;
  b = buScale * u;

  yTL *= yScale; yTR *= yScale;
  yBL *= yScale; yBR *= yScale;

  if (bits == 24) {
    /* Write out top two pixels */
    //rgb[0] = LIMIT(b+yTL); rgb[1] = LIMIT(g+yTL); rgb[2] = LIMIT(r+yTL);
    //rgb[3] = LIMIT(b+yTR); rgb[4] = LIMIT(g+yTR); rgb[5] = LIMIT(r+yTR);
    rgb[2] = LIMIT(b+yTL); rgb[1] = LIMIT(g+yTL); rgb[0] = LIMIT(r+yTL);
    rgb[5] = LIMIT(b+yTR); rgb[4] = LIMIT(g+yTR); rgb[3] = LIMIT(r+yTR);

    /* Skip down to next line to write out bottom two pixels */
    rgb += 3 * rowPixels;
    //rgb[0] = LIMIT(b+yBL); rgb[1] = LIMIT(g+yBL); rgb[2] = LIMIT(r+yBL);
    //rgb[3] = LIMIT(b+yBR); rgb[4] = LIMIT(g+yBR); rgb[5] = LIMIT(r+yBR);
    rgb[2] = LIMIT(b+yBL); rgb[1] = LIMIT(g+yBL); rgb[0] = LIMIT(r+yBL);
    rgb[5] = LIMIT(b+yBR); rgb[4] = LIMIT(g+yBR); rgb[3] = LIMIT(r+yBR);
  } else if (bits == 16) {
    /* Write out top two pixels */
    rgb[0] = ((LIMIT(b+yTL) >> 3) & 0x1F) | ((LIMIT(g+yTL) << 3) & 0xE0);
    rgb[1] = ((LIMIT(g+yTL) >> 5) & 0x07) | (LIMIT(r+yTL) & 0xF8);

    rgb[2] = ((LIMIT(b+yTR) >> 3) & 0x1F) | ((LIMIT(g+yTR) << 3) & 0xE0);
    rgb[3] = ((LIMIT(g+yTR) >> 5) & 0x07) | (LIMIT(r+yTR) & 0xF8);

    /* Skip down to next line to write out bottom two pixels */
    rgb += 2 * rowPixels;

    rgb[0] = ((LIMIT(b+yBL) >> 3) & 0x1F) | ((LIMIT(g+yBL) << 3) & 0xE0);
    rgb[1] = ((LIMIT(g+yBL) >> 5) & 0x07) | (LIMIT(r+yBL) & 0xF8);

    rgb[2] = ((LIMIT(b+yBR) >> 3) & 0x1F) | ((LIMIT(g+yBR) << 3) & 0xE0);
    rgb[3] = ((LIMIT(g+yBR) >> 5) & 0x07) | (LIMIT(r+yBR) & 0xF8);
  }
}

/*
 */
static inline void
v4l_copy_422_block (int yTL, int yTR, int u, int v, 
		    int rowPixels, unsigned char * rgb, int bits)
{
  const int rvScale = 91881;
  const int guScale = -22553;
  const int gvScale = -46801;
  const int buScale = 116129;
  const int yScale  = 65536;
  int r, g, b;

  g = guScale * u + gvScale * v;
  r = rvScale * v;
  b = buScale * u;

  yTL *= yScale; yTR *= yScale;

  if (bits == 24) {
    /* Write out top two pixels */
    rgb[0] = LIMIT(b+yTL); rgb[1] = LIMIT(g+yTL); rgb[2] = LIMIT(r+yTL);
    rgb[3] = LIMIT(b+yTR); rgb[4] = LIMIT(g+yTR); rgb[5] = LIMIT(r+yTR);

  } else if (bits == 16) {
    /* Write out top two pixels */
    rgb[0] = ((LIMIT(b+yTL) >> 3) & 0x1F) | ((LIMIT(g+yTL) << 3) & 0xE0);
    rgb[1] = ((LIMIT(g+yTL) >> 5) & 0x07) | (LIMIT(r+yTL) & 0xF8);

    rgb[2] = ((LIMIT(b+yTR) >> 3) & 0x1F) | ((LIMIT(g+yTR) << 3) & 0xE0);
    rgb[3] = ((LIMIT(g+yTR) >> 5) & 0x07) | (LIMIT(r+yTR) & 0xF8);
  }
}

/*
 * convert a YUV420P to a rgb image
 */
int
v4l_yuv420p2rgb (unsigned char *rgb_out, unsigned char *yuv_in,
		 int width, int height, int bits)
{
  const int numpix = width * height;
  const unsigned int bytes = bits >> 3;
  int h, w, y00, y01, y10, y11, u, v;
  unsigned char *pY = yuv_in;
  unsigned char *pU = pY + numpix;
  unsigned char *pV = pU + numpix / 4;
  unsigned char *pOut = rgb_out;
  //FILE *debug = fopen("/root/color.txt", "w");
  //unsigned char *pOu;

  if (!rgb_out || !yuv_in)
    return -1;

  for (h = 0; h <= height - 2; h += 2) {
    for (w = 0; w <= width - 2; w += 2) {
      y00 = *(pY);
      y01 = *(pY + 1);
      y10 = *(pY + width);
      y11 = *(pY + width + 1);
      u = (*pU++) - 128;
      v = (*pV++) - 128;
      
      v4l_copy_420_block (y00, y01, y10, y11, u, v, width, pOut, bits);
      /*fprintf(debug, "%3d %3d %3d %3d,  %3d %3d: %3d %3d %3d; %3d %3d %3d; ", y00, y01, y10, y11, u, v, pOut[0], pOut[1], pOut[2], pOut[3], pOut[4], pOut[5]);
      pOu = pOut + 3*width;
      fprintf(debug, "%3d %3d %3d; %3d %3d %3d;\n", pOu[0], pOu[1], pOu[2], pOu[3], pOu[4], pOu[5]);
      */
      pY += 2;
      pOut += bytes << 1;

    }
    pY += width;
    pOut += width * bytes;
  }
  //fclose(debug);
  return 0;
}

/*
 * convert a YUV422P to a rgb image
 */
int
v4l_yuv422p2rgb (unsigned char *rgb_out, unsigned char *yuv_in,
		 int width, int height, int bits)
{
  const int numpix = width * height;
  const unsigned int bytes = bits >> 3;
  int h, w, y00, y01, u, v;
  unsigned char *pY = yuv_in;
  unsigned char *pU = pY + numpix;
  unsigned char *pV = pU + numpix / 2;
  unsigned char *pOut = rgb_out;

  if (!rgb_out || !yuv_in)
    return -1;

  for (h = 0; h < height; h += 1) {
    for (w = 0; w <= width - 2; w += 2) {
      y00 = *(pY);
      y01 = *(pY + 1);
      u = (*pU++) - 128;
      v = (*pV++) - 128;

      v4l_copy_422_block (y00, y01, u, v, width, pOut, bits);
	
      pY += 2;
      pOut += bytes << 1;

    }
    //pY += width;
    //pOut += width * bytes;
  }
  return 0;
}
/*  PNG Code for PDAs See Other Version For Working Function
//error_handler that does nothing for WritePNGtoFileRGB
static void error_handler(struct png_struct_def *, const char *error)
{
  throw error;
}
//same as above
static void warning_handler(struct png_struct_def *, const char *warning)
{
}

//writes a PNG to file pngtest.png
int WritePNGToFile(void *data, int nx, int ny)
{ 
  unsigned char* rawRGB = (unsigned char *)data;
  FILE *outfile;
  if((outfile = fopen("/root/pngtest.png", "w+")) == NULL) 
  {
    printf("bad file.  bad bad file!\n");
    return 0;
  }
  png_structp png_ptr;
  png_infop info_ptr;
  png_ptr = png_create_write_struct(PNG_LIBPNG_VER_STRING, (png_voidp)error_handler, error_handler, warning_handler);

  if(png_ptr == NULL)
    return 1;

  info_ptr = png_create_info_struct(png_ptr);
  if (info_ptr == NULL)
  {
    png_destroy_write_struct(&png_ptr, (png_infopp)NULL);
    return 1;
  }

  if (setjmp(png_jmpbuf(png_ptr)))
  {
    png_destroy_write_struct(&png_ptr, &info_ptr);
  
    return 1;
  }

  //init file output
  png_init_io(png_ptr, outfile);
  png_set_IHDR(png_ptr, info_ptr, nx, ny, 8, PNG_COLOR_TYPE_RGB, PNG_INTERLACE_NONE, PNG_COMPRESSION_TYPE_DEFAULT, PNG_FILTER_TYPE_DEFAULT);
  png_bytep row_pointers[ny];
  png_write_info(png_ptr, info_ptr);
  int rowSize = nx*4;
  for(int i = 0; i < ny; i++)
    row_pointers[i] = (png_bytep)(rawRGB+i*rowSize); //i don't know what pixel size is...but I'm guessing its 4...
  png_write_image(png_ptr, row_pointers);
  png_write_end(png_ptr, info_ptr);
  png_destroy_write_struct(&png_ptr, &info_ptr);
  fclose(outfile);

  return 0;
}
*/
/*************************************************************************\
Code from:
http://www.cactuscode.org/Grdoc/CactusStable/CactusIO/PROTECTED/IOJpeg/src/JPEG_c_src.html
\*************************************************************************/


/*
  Image data is an array of unsigned character array of
  RGB data.  The data is stored in interleaved order.
  IE, the first three elements are a byte of Red followed
  by a byte of Green and then a byte of Blue for the first
  pixel.  Data is stored in fortran order (ie. x is fastest
  moving dimension).
 */
int WriteJPEGToFileRGB(int nx, /* width of image in pixels */
		       int ny, /* height of the image in pixels */
		       void *data, /* buffer containing image data */
		       int Quality, /* Integer from 0 to 100 */
		       FILE* outfile){	/* name of file to store in */
  JpgComp cinfo;
  JpgErr jerr;  
  /*  FILE * outfile;*/
  unsigned char *dataRGB = (unsigned char *)data;
  JSAMPROW row_pointer=(JSAMPROW)dataRGB;
  
  memset (&cinfo,0,sizeof(cinfo));
  cinfo.err = jpeg_std_error(&jerr);
  jpeg_create_compress(&cinfo);
  	
  /* Setup JPEG */
  cinfo.image_width = nx ; 	/* image width and height, in pixels */
  cinfo.image_height = ny;
  cinfo.input_components = 3;	/* # of color components per pixel=3 RGB */
  cinfo.in_color_space = JCS_RGB;
  /*  if ((outfile = fopen(FileName, "wb")) == NULL) {
    printf("Cannot open file [%s]\n",FileName);
    return 0; 
  } */
  jpeg_stdio_dest(&cinfo, outfile);
  jpeg_set_defaults(&cinfo);
  jpeg_set_quality (&cinfo,Quality,TRUE);
  /* Starting compress */
  jpeg_start_compress(&cinfo, TRUE);
  /* Now compress everything one scanline at-a-time */
  while (cinfo.next_scanline < cinfo.image_height) {
    row_pointer = (JSAMPROW)(dataRGB+(cinfo.next_scanline*3*nx)); 
    jpeg_write_scanlines(&cinfo, &row_pointer, 1);
  }
  jpeg_finish_compress(&cinfo);
  jpeg_destroy_compress(&cinfo);
  /* All done! */
  /*  fclose(outfile);*/
  return 1;
}


/*--------------
  A hack to hijack JPEG's innards to write into a memory buffer
----------------
/  this defines a new destination manager to store images in memory
/  derived by jdatadst.c */
typedef struct {
  struct jpeg_destination_mgr pub;	/* public fields */
  JOCTET *buffer;					/* start of buffer */
  int bufsize;						/* buffer size */
  int datacount;					/* finale data size */
} memory_destination_mgr;

typedef memory_destination_mgr *mem_dest_ptr;

/*----------------------------------------------------------------------------
  Initialize destination --- called by jpeg_start_compress before
  any data is actually written. */

METHODDEF(void)
init_destination (j_compress_ptr cinfo)
{
  mem_dest_ptr dest = (mem_dest_ptr) cinfo->dest;

  dest->pub.next_output_byte = dest->buffer;
  dest->pub.free_in_buffer = dest->bufsize;
  dest->datacount=0;

  //printf("INIT\n");
}



/*----------------------------------------------------------------------------
  /  Empty the output buffer --- called whenever buffer fills up. */
METHODDEF(boolean)
empty_output_buffer (j_compress_ptr cinfo)
{
  mem_dest_ptr dest = (mem_dest_ptr) cinfo->dest;

  dest->pub.next_output_byte = dest->buffer;
  dest->pub.free_in_buffer = dest->bufsize;
  
  printf("EMPTY BUFFER\n");
    
  return TRUE;
}


/*----------------------------------------------------------------------------

  /  Terminate destination --- called by jpeg_finish_compress
  /  after all data has been written.  Usually needs to flush buffer. */
METHODDEF(void)
term_destination (j_compress_ptr cinfo)
{
  /* expose the finale compressed image size */
  
  mem_dest_ptr dest = (mem_dest_ptr) cinfo->dest;
  dest->datacount = dest->bufsize - dest->pub.free_in_buffer;
  //printf("END!\n");
}


/*----------------------------------------------------------------------------
/ Prepare for output to a memory buffer. The caller must have allocate memory
/ to store the compressed image, and supply its size */
GLOBAL(void)
jpeg_memory_dest (j_compress_ptr cinfo, JOCTET *buffer,int bufsize)
{
  mem_dest_ptr dest;
  if (cinfo->dest == NULL) {	/* first time for this JPEG object? */
    cinfo->dest = (struct jpeg_destination_mgr *)
      (*cinfo->mem->alloc_small) ((j_common_ptr) cinfo, JPOOL_PERMANENT,
				  sizeof(memory_destination_mgr));
  }

  dest = (mem_dest_ptr) cinfo->dest;
  dest->bufsize=bufsize;
  dest->buffer=buffer;
  dest->pub.init_destination = init_destination;
  dest->pub.empty_output_buffer = empty_output_buffer;
  dest->pub.term_destination = term_destination;
}

/********************************************
Identical in nearly every way to WriteJPEGToFileRGB(), but
it writes into the memory buffer specified.  To be safe, its
good to make the memorybuffer the same size as the input image
+ 1024 bytes.  It is guaranteed that the image will be less
than this size.  In fact, if you use a typical "quality" level
of 75, you can get away with an image which is one quarter that
size.

 ******************************************** */
int WriteJPEGToMemoryRGB(int nx,int ny, void *data, int Quality, char 
			 *memorybuffer,int bufsize){	
  JpgComp cinfo;
  JpgErr jerr;  
  unsigned char *dataRGB = (unsigned char *)data;
  JSAMPROW row_pointer=(JSAMPROW)dataRGB;

  //For memory buffer
  JOCTET *jpgbuff;
  mem_dest_ptr dest;
  int csize=0;
  memset(memorybuffer, 0, bufsize);

  /* zero out the compresion info structures and
     allocate a new compressor handle */
  memset (&cinfo,0,sizeof(cinfo));
  cinfo.err = jpeg_std_error(&jerr);
  jpeg_create_compress(&cinfo);
 
  /* Setup JPEG datastructures */
  cinfo.image_width = nx ; 	/* image width and height, in pixels */
  cinfo.image_height = ny;
  cinfo.input_components = 3;	/* # of color components per pixel=3 RGB */
  cinfo.in_color_space = JCS_RGB;
  //cinfo.dct_method = JDCT_ISLOW; //different compression algo

  //For memory buffer
  jpgbuff = (JOCTET*)memorybuffer;

  /* Setup compression and do it */
  jpeg_memory_dest(&cinfo,jpgbuff,bufsize);
  jpeg_set_defaults(&cinfo);
  jpeg_set_quality (&cinfo,Quality,TRUE);
  jpeg_start_compress(&cinfo, TRUE);
  /* compress each scanline one-at-a-time */
  while (cinfo.next_scanline < cinfo.image_height) {
    row_pointer = (JSAMPROW)(dataRGB+(cinfo.next_scanline*3*nx));
    jpeg_write_scanlines(&cinfo, &row_pointer, 1);
  }
  jpeg_finish_compress(&cinfo);
  /* Now extract the size of the compressed buffer */
  dest=(mem_dest_ptr)cinfo.dest;
  csize=dest->datacount; /* the actual compressed datasize */
  /* destroy the compressor handle */
  jpeg_destroy_compress(&cinfo);

  //printf("Image Size is %d\n",csize);
  return csize;
}
