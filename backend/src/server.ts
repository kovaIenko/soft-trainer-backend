import express from 'express';
import cors from 'cors';

import githubRoutes from './routes/github-routes';
import googleRoutes from './routes/google-routes';
import dellApi from './controllers/dell-api';

const PORT = process.env.PORT || 3001;

const app = express();

app.use(
  cors({
    origin: ['*'],
    methods: 'GET,POST',
  }),
);

app.use(express.json());

app.use('/api/github', githubRoutes);
app.use('/api/google', googleRoutes);
app.use('/api', dellApi);

app.listen(PORT, () => console.log('Server on port', PORT));
