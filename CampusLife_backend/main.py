import os
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from sentence_transformers import SentenceTransformer
from google import genai
from google.genai import types
import uvicorn

# Initialize FastAPI App
app = FastAPI(title="CampusLife AI RAG Backend")

# 1. Load the local embedding model
print("Loading all-MiniLM-L6-v2 model...")
embedding_model = SentenceTransformer("all-MiniLM-L6-v2")
print("Model loaded successfully!")

# 2. Retrieve Gemini API Key safely from environment or use direct fallback string
GEMINI_KEY = os.environ.get("GEMINI_API_KEY", "")

if not GEMINI_KEY:
    print("\n⚠️ WARNING: GEMINI_API_KEY environment variable not detected!")
    print("Please ensure you run 'export GEMINI_API_KEY=\"your_key\"' in your terminal before running uvicorn.\n")

# Initialize Gemini client passing the structural token configuration explicitly
try:
    if GEMINI_KEY:
        ai_client = genai.Client(api_key=GEMINI_KEY)
    else:
        # Fallback to default structural construction strategy
        ai_client = genai.Client()
    print("Gemini GenAI client linked successfully!")
except Exception as e:
    print(f"Critical: Gemini Client initialization failed. Error: {e}")

# --- Pydantic Data Schemas ---

class NotePayload(BaseModel):
    text: str

class EmbeddingResponse(BaseModel):
    embedding: list[float]

# Note context payload along with user queries matching Android data properties
class AgentRequest(BaseModel):
    query: str
    context: str 

class AgentResponse(BaseModel):
    answer: str


# --- API Endpoints ---

@app.post("/get_embedding/", response_model=EmbeddingResponse)
async def get_embedding(payload: NotePayload):
    """
    Accepts text context, vectorizes it via all-MiniLM-L6-v2, 
    and returns a 384-dimensional floating-point array.
    """
    try:
        if not payload.text.strip():
            raise HTTPException(status_code=400, detail="Text payload cannot be empty.")
        
        # Generate embedding vector
        vector = embedding_model.encode(payload.text).tolist()
        return EmbeddingResponse(embedding=vector)
        
    except Exception as e:
        print(f"[ERROR] /get_embedding/ pipeline exception: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Embedding Generation Error: {str(e)}")


@app.post("/ask/", response_model=AgentResponse)
@app.post("/askAgent/", response_model=AgentResponse) # 👈 Dual mapping fallback handles both paths seamlessly
async def ask_agent(request: AgentRequest):
    """
    Receives the user query alongside the matched context payload from Android,
    compiles a structured prompt block, and returns precise answers.
    """
    try:
        user_query = request.query
        context_data = request.context

        if not user_query.strip():
            raise HTTPException(status_code=400, detail="Query prompt cannot be empty.")

        # Structure the final execution prompt payload explicitly separating data blocks
        compiled_prompt = (
            f"Context sources from phone storage:\n"
            f"{context_data}\n\n"
            f"User Question: {user_query}"
        )

        # System instructions configured for strict prompt grounding
        system_instruction = (
            "You are the CampusLife AI Assistant, an elite university companion tool.\n"
            "You are processing text payload blocks containing note models sourced from the user's phone database storage.\n"
            "CRITICAL DIRECTIVES:\n"
            "1. Core answers MUST be built entirely out of the provided structured data strings under 'Context sources from phone storage'.\n"
            "2. Carefully inspect all attributes like 'Title', 'Content', and '[Extracted Attachment Data]' sections to trace facts.\n"
            "3. If a user asks for a teacher name, room identifier, schedule, or text information, parse out the corresponding record explicitly.\n"
            "4. Never say you do not have the content if the facts are in the text block. Stay concise, crisp, and direct."
        )

        # Call Gemini model instance
        response = ai_client.models.generate_content(
            model='gemini-2.5-flash',
            contents=compiled_prompt,
            config=types.GenerateContentConfig(
                system_instruction=system_instruction,
                temperature=0.1
            )
        )

        ai_response_text = response.text if response.text else "The assistant could not map a response cleanly."
        return AgentResponse(answer=ai_response_text)

    except Exception as e:
        # Prints the precise traceback stack in your terminal window before throwing the server error code
        print(f"\n❌ [CRITICAL BACKEND ERROR] Exception intercepted inside /ask/ handler block:")
        print(f"Details: {str(e)}\n")
        raise HTTPException(status_code=500, detail=f"Agent Processing Error: {str(e)}")


# --- Health Check ---
@app.get("/")
def read_root():
    return {"status": "online", "project": "CampusLife Full-Stack RAG Engine"}


if __name__ == "__main__":
    # Ensure standard script module initialization name aligns with filename execution limits
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)