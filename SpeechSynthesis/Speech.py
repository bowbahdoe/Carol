from speak import say
class Speech(Object):
    def __init__(self, text):
        //No function assigned to onFinish
        self.onFinish = None
        self.onFinishArguments = None

        self.text = text
    def speak:
        say(self.text)
        self.onFinish()
