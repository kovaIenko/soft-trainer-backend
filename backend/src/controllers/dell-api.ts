const express = require('express');
import { Request, Response, Router } from 'express';
const axios = require('axios');
const multer  = require('multer')

const upload = multer({ dest: 'uploads/' })
const fs = require('fs');
const sharp = require('sharp');


//const { OAuth2Client } = require('google-auth-library');

const { Configuration, OpenAIApi } = require("openai");

const configuration = new Configuration({
  apiKey: process.env.DELL_API_TOKEN,
});
const openai = new OpenAIApi(configuration);

const router: Router = express.Router();

//const client = new OAuth2Client(process.env.VITE_GOOGLE_CLIENT_ID);

// const verifyGoogleAuthToken = async (req: Request, res: Response, next: NextFunction) => {
//   const authTokenHeader = req.headers.authorization;
//   if (!authTokenHeader) {
//     return res.status(401).send('Missing authorization token');
//   }

//   const authTokenSegments = authTokenHeader.split(' ');
//   if (authTokenSegments.length !== 2) {
//     return res.status(401).send('Invalid authorization token');
//   }
//   const idToken = authTokenSegments[1];;
//   console.log(idToken)
//   try {
//     const ticket = await client.verifyIdToken({
//       idToken: idToken,
//       audience: process.env.VITE_GOOGLE_CLIENT_ID
//     });

//     console.log(ticket)
//     const payload = ticket.getPayload();
//     console.log(payload);
//     const userId = payload['sub'];
//     console.log(userId);
//     return next();
//   } catch (err: any) {
//     console.error(err);
//     return res.status(401).send('Invalid authorization token');
//   }
// };

router.post('/txt2img', async (req: Request, res: Response) => {
  console.log("Sent request to txt/img")
  const { prompt } = req.body;
  try {
    const response = await openai.createImage({
      prompt: prompt});

const imageUrl = response.data.data[0].url;
console.log(imageUrl)

const imageResponse = await axios.get(imageUrl, {
  responseType: 'arraybuffer',
});

const base64Image = Buffer.from(imageResponse.data, 'binary').toString('base64');

// Send the base64 image as a response  
res.send({ image: base64Image });

  } catch (error: any) {
    console.error(error);
    console.log(error.response.data)
    if (
      error.response &&
      error.response.data &&
      error.response.data.message &&
      error.response.data.message.includes('Missing image file')
    ) {
      res.status(400).send({ message: 'Image file is missing from the request' });
    } else {
      res.status(500).send({ message: 'Internal Server Error' });
    }
  }
});

router.post('/img2img', upload.single('image'), async (req: Request, res: Response) => {
  console.log("Sent request to img/img")
  try {
    const {prompt} = req.body;
    const image  = req.file;
    console.log(image)

if(image){
  console.log(image)
    const ps = './uploads/' + image.filename.replace(/\.[^/.]+$/, "") + '.png';

    await sharp(image.path).ensureAlpha().toFormat('png')
    .toFile(ps);

    console.log(sharp(ps).metadata())

    const response = await openai.createImageEdit(
      fs.createReadStream(ps),
      prompt
      );

    console.log(response.data);

    const imageUrl = response.data.data[0].url;
    console.log(imageUrl)

    //const base64Image = getBase64ImageFromURL(imageUrl);

    const imageResponse = await axios.get(imageUrl, {
      responseType: 'arraybuffer',
    });

    const base64Image = Buffer.from(imageResponse.data, 'binary').toString('base64');

    // Send the base64 image as a response
    res.send({ image: base64Image });
  }
  } catch (error: any) {
    console.error(error);
    console.log(error.response.data)
    if (
      error.response &&
      error.response.data &&
      error.response.data.message &&
      error.response.data.message.includes('Missing image file')
    ) {
      res.status(400).send({ message: 'Image file is missing from the request' });
    } else {
      res.status(500).send({ message: 'Internal Server Error' });
    }
  }
});

export default router;
