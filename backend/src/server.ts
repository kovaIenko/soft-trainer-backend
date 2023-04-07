import express from 'express';
import cors from 'cors';

import githubRoutes from './routes/github-routes';
import googleRoutes from './routes/google-routes';
import dellApi from './controllers/dell-api';

const PORT = process.env.PORT || 3001;

const CLIENT_URL="http://ec2-54-151-84-190.us-west-1.compute.amazonaws.com:5173";

const app = express();

app.use(
  cors({
    origin: [CLIENT_URL],
    methods: 'GET,POST',
  }),
);

app.use(express.json());

app.use('/api/github', githubRoutes);
app.use('/api/google', googleRoutes);
app.use('/api', dellApi);

app.listen(PORT, () => console.log('Server on port', PORT));
