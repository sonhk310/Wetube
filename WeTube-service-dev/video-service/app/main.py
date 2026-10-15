from fastapi import FastAPI, UploadFile, File, HTTPException
from fastapi.responses import JSONResponse
import shutil
import os
from utils.audio_processing import extract_audio, transcribe_audio

app = FastAPI()

UPLOAD_DIR = "uploads"
os.makedirs(UPLOAD_DIR, exist_ok=True)

@app.post("/transcribe")
async def transcribe_video(file: UploadFile = File(...)):
    # Lưu video tạm thời
    video_path = os.path.join(UPLOAD_DIR, file.filename)
    with open(video_path, "wb") as buffer:
        shutil.copyfileobj(file.file, buffer)

    # Tạo file audio tạm thời
    audio_path = os.path.join(UPLOAD_DIR, f"{file.filename}.wav")

    try:
        extract_audio(video_path, audio_path)
        transcript = transcribe_audio(audio_path)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
    finally:
        # Cleanup
        if os.path.exists(video_path):
            os.remove(video_path)
        if os.path.exists(audio_path):
            os.remove(audio_path)

    return JSONResponse(content={"transcript": transcript})
