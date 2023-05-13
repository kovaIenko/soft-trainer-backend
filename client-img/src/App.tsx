import { BrowserRouter as Router, Route, Routes } from 'react-router-dom';
import { Login, Home, Chat } from './pages';

const App = () => {
  return (
    <Router>
      <Routes>
        <Route path="/chat" element={<Home />}></Route>
        <Route path="/" element={<Login />}></Route>
        <Route path="/home" element={< Chat/>}></Route>
      </Routes>
    </Router>
  );
};

export default App;
