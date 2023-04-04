const express = require('express');
const axios = require('axios');
// const fs = require('fs');

// const path = require('path');

//const { OAuth2Client } = require('google-auth-library');
import { Request, Response, Router } from 'express';

const router: Router = express.Router();

//const client = new OAuth2Client(process.env.VITE_GOOGLE_CLIENT_ID);

// const verifyGoogleAuthToken = async (req: Request, res: Response, next: NextFunction) => {
//   const authTokenHeader = req.headers.authorization;
//   if (!authTokenHeader) {
//     return res.status(401).send('Missing authorization token');
//   }
//   const authToken = authTokenHeader.split(' ')[1];
//   console.log(authToken)
//   try {
//     const ticket = await client.verifyIdToken({
//       idToken: authToken,
//       audience: process.env.VITE_GOOGLE_CLIENT_ID
//     });
//     const payload = ticket.getPayload();
//     console.log(payload);
//     const userId = payload['sub'];
//     console.log(userId);
//     return next();
//   } catch (err) {
//     console.error(err);
//     return res.status(401).send('Invalid authorization token');
//   }
// };

router.post('/txt2img', async (req: Request, res: Response) => {
  const { prompt } = req.body;
  console.log(prompt)
  const formData = new FormData();
  formData.append('prompt', prompt);

  try {
     // Send a request to the Dell API to generate an image
const response = await axios.post('https://api.openai.com/v1/images/generations', formData, {
  headers: {
    'Content-Type': 'application/json',
    Authorization: `Bearer ${process.env.DELL_API_TOKEN}`,
  }
});

const imageUrl = response.data.data[0].url;
//console.log(imageUrl)
// Download the PNG image
const imageResponse = await axios.get(imageUrl, {
  responseType: 'arraybuffer',
});

// Convert the image to base64
const base64Image = Buffer.from(imageResponse.data, 'binary').toString('base64');

// Send the base64 image as a response
res.send({ image: base64Image });

    
  } catch (error) {
    console.error(error);
    res.status(500).send({ message: 'Internal Server Error' });
  }
});

export default router;
