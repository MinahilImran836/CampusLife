import os
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from sentence_transformers import SentenceTransformer
from google import genai
from google.genai import types
import uvicorn
from dotenv import load_dotenv

load_dotenv()
app = FastAPI(title="CampusLife AI RAG Backend")

# Initialize model
print("Loading all-MiniLM-L6-v2 model...")
embedding_model = SentenceTransformer("all-MiniLM-L6-v2")

GEMINI_KEY = os.environ.get("GEMINI_API_KEY")
ai_client = genai.Client(api_key=GEMINI_KEY) if GEMINI_KEY else genai.Client()

class NotePayload(BaseModel): text: str
class EmbeddingResponse(BaseModel): embedding: list[float]
class AgentRequest(BaseModel): query: str; context: str 
class AgentResponse(BaseModel): answer: str

@app.post("/get_embedding/")
async def get_embedding(payload: NotePayload):
    vector = embedding_model.encode(payload.text).tolist()
    return {"embedding": vector}

@app.post("/ask/")
async def ask_agent(request: AgentRequest):
    # Log incoming request
    print(f"--- RAG PIPELINE ---")
    print(f"Query: {request.query}")
    print(f"Context Length: {len(request.context)}")
    
    if not request.query.strip():
        raise HTTPException(status_code=400, detail="Query empty")

    # If context is empty, guide the model appropriately
    context_str = request.context if request.context.strip() else "No specific local notes found."
    
    compiled_prompt = f"Context sources from phone storage:\n{context_str}\n\nUser Question: {request.query}"
    
    try:
        response = ai_client.models.generate_content(
            model='gemini-2.5-flash',
            contents=compiled_prompt,
            config=types.GenerateContentConfig(
                system_instruction=(
                    "You are the CampusLife AI Assistant. Core answers MUST be built entirely "
                    "out of the provided 'Context sources from phone storage'. If the context is "
                    "empty or irrelevant, state that you cannot find the answer in the user's notes."
                ),
                temperature=0.1
            )
        )
        return {"answer": response.text or "The assistant could not map a response."}
    except Exception as e:
        print(f"AI Error: {e}")
        raise HTTPException(status_code=500, detail=str(e))

if __name__ == "__main__":
    uvicorn.run("main:app", host="0.0.0.0", port=int(os.environ.get("PORT", 8000)), reload=True)