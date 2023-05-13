import express from 'express';
import cors from 'cors';

import githubRoutes from './routes/github-routes';
import googleRoutes from './routes/google-routes';
import openApiRoutes from './controllers/open-api-controller';
import chatsRoutes from './controllers/chats-controller';
import messagesRoutes from './controllers/messages-controller';

const PORT = process.env.PORT || 3001;

const CLIENT_URL="http://ec2-54-151-84-190.us-west-1.compute.amazonaws.com:5173";

const app = express();

app.use(
  cors({
    origin: [CLIENT_URL, "http://localhost:5173"],
    methods: 'GET,POST',
  }),
);

app.use(express.json());

app.use('/api/github', githubRoutes);
app.use('/api/google', googleRoutes);
app.use('/api', openApiRoutes);
app.use('/api/chats', chatsRoutes)
app.use('/api/messages', messagesRoutes)

app.listen(PORT, () => console.log('Server on port', PORT));
