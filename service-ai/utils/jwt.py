from fastapi import HTTPException
from jose import jwt, JWTError
from config import settings
import base64

SECRET_KEY = base64.b64decode(settings.jwt_secret_key)
ALGORITHM = settings.jwt_algorithm

def verify_email_from_token(authorization: str, email: str):
    try:
        scheme, token = authorization.split()
        if scheme.lower() != "bearer":
            raise HTTPException(status_code=401, detail="Invalid token.")

        payload = jwt.decode(token, SECRET_KEY, algorithms=[ALGORITHM])
        token_email = payload.get("email")

        if token_email is None:
            raise HTTPException(status_code=401, detail="Token does not contain email claim")

        if token_email != email:
            raise HTTPException(status_code=403, detail="Email in token does not match request parameter")

    except JWTError:
        raise HTTPException(status_code=401, detail="Invalid JWT token")
    except Exception:
        raise HTTPException(status_code=401, detail="Invalid or missing Authorization header")
