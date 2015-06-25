name=`uname`
if [[ "$name" == 'Linux' ]]; then
  sudo apt-get install flite python-pyaudio flac -y
  sudo pip install SpeechRecognition python-forecastio
elif [[ "$name" == 'Darwin' ]]; then
  pip install pyaudio SpeechRecognition python-forecastio
  brew install flac
fi
