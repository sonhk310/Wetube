from moviepy.editor import VideoFileClip
from google.cloud import speech
import os

def extract_audio(video_path: str, audio_path: str):

    clip = VideoFileClip(video_path)
    clip.audio.write_audiofile(audio_path, fps=16000, codec='pcm_s16le')
    clip.close()
    return audio_path

def transcribe_audio(audio_path: str) -> str:

    client = speech.SpeechClient()

    with open(audio_path, "rb") as f:
        audio_bytes = f.read()

    audio = speech.RecognitionAudio(content=audio_bytes)
    config = speech.RecognitionConfig(
        encoding=speech.RecognitionConfig.AudioEncoding.LINEAR16,
        sample_rate_hertz=16000,
        language_code="en-US",
    )

    response = client.recognize(config=config, audio=audio)

    transcript = " ".join([result.alternatives[0].transcript for result in response.results])
    return transcript
