import axios from "axios";

async function verifyToken(token: string) {
    const url = `https://oauth2.googleapis.com/tokeninfo?access_token=${token}`;
    try {
      const response = await axios.get(url);
      //console.log(response.data)
      return response.data;
    } catch (error: any) {
      console.error(`Failed to verify token: ${error.response.status} ${error.response.statusText}`);
      return null;
    }
  }

  export default verifyToken;
