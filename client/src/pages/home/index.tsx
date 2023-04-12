import { useEffect, useRef, useState } from "react"
import { useNavigate } from "react-router-dom"
//import { Button, Col, Container, Navbar, Row, Text, User } from "@nextui-org/react"
import { User } from "@nextui-org/react"
import axios from "axios"

import 'bootstrap/dist/css/bootstrap.min.css';
import { Navbar, Button, Spinner} from 'react-bootstrap';

import { getAccessTokenGithub, getUserDataGithub, getUserDataGoogle } from "./services/home-services"

import { LogOutIcon } from "../../assets/icons"

const SERVER_ENDPOINT_URL='http://ec2-54-193-47-189.us-west-1.compute.amazonaws.com:3001';
//const SERVER_ENDPOINT_URL="http://localhost:3001";

interface UserDataGithub {
	avatar_url: string
	login: string
	bio: string
}

interface UserdataGoogle {
	name: string
	picture: string
	email: string
}

const Home = () => {
	const [userDataGithub, setUserDataGithub] = useState<null | UserDataGithub>(null)
	const [userDataGoogle, setUserDataGoogle] = useState<null | UserdataGoogle>(null)

	const [prompt, setPrompt] = useState('');
	const [prompt2, setPrompt2] = useState('');
	const [images, setImages] = useState<string | null>();
	const [image, setImage] = useState<File | null>(null);
	const [generatedImage, setGeneratedImage] = useState<string | null>();

	const [loading, setLoading] = useState(false);
	const [loading2, setLoading2] = useState(false);
  

	const loginWith = useRef(localStorage.getItem("loginWith"))

	const navigate = useNavigate()

	useEffect(() => {
		const queryString = window.location.search
		const urlParams = new URLSearchParams(queryString)
		const codeParam = urlParams.get("code")

		const accessToken = localStorage.getItem("accessToken")

		if (codeParam && !accessToken && loginWith.current === "GitHub") {
			getAccessTokenGithub(codeParam).then(resp => {
				localStorage.setItem("accessToken", resp.access_token)
				getUserDataGithub(resp.access_token).then((resp: UserDataGithub) => {
					setUserDataGithub(resp)
				})
			})
		} else if (codeParam && accessToken && loginWith.current === "GitHub") {
			getUserDataGithub(accessToken).then((resp: UserDataGithub) => {
				localStorage.setItem("accessToken", accessToken)
				setUserDataGithub(resp)
			})
		}
	}, [loginWith])

	useEffect(() => {
		const accessToken = localStorage.getItem("accessToken")

		if (accessToken && loginWith.current === "Google") {
			getUserDataGoogle(accessToken).then(resp => {
				setUserDataGoogle(resp)
			})
		}
	}, [loginWith])

	const setLogOut = () => {
		localStorage.removeItem("accessToken")
		localStorage.removeItem("loginWith")
		navigate("/")
	}

	if (!userDataGithub && !userDataGoogle) return null

  const handlePromptChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    setPrompt(event.target.value);
  };

  const handlePromptChange2 = (event: React.ChangeEvent<HTMLInputElement>) => {
    setPrompt2(event.target.value);
  };

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
	setLoading(true);
	setImages(null);
    const response = await fetch(`${SERVER_ENDPOINT_URL}/api/txt2img`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
		'Authorization': `Bearer ${localStorage.getItem("accessToken")}`,
		"Access-Control-Allow-Origin": "*"
      },
      body: JSON.stringify({prompt})
    });

	const data = await response.json();
	console.log(data.image)
	setImages(`data:image/png;base64,${data.image}`);
	//console.log(images)
	setLoading(false);
  };

  const handleImageChange = (e: React.ChangeEvent<HTMLInputElement>) => {
	const file = e.target.files?.[0];
	if (file) {
	  setImage(file);
	}
  };

  const handleImageSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
	event.preventDefault();
	if (image) {
		setLoading2(true);
		setGeneratedImage(null)
		const formData = new FormData();
		formData.append("prompt", prompt2);
		formData.append("image", image, image.name);
		try {
		  const response = await axios.post(`${SERVER_ENDPOINT_URL}/api/img2img`, formData, {
			headers: {
				'Content-Type': 'multipart/form-data',
				'Authorization': `Bearer ${localStorage.getItem("accessToken")}`,
		         "Access-Control-Allow-Origin": "*"
			},
		  });
		      // Extract the base64 string from the response data, if it exists
		const base64String = response?.data?.image ?? '';

		setGeneratedImage(`data:image/png;base64,${base64String}`);
		setLoading2(false);
		} catch (error: any) {
		  console.error(error);
		  console.log(error.response.data)
		  setLoading2(false);
		}
	  }
  };

	return (
		<>
		<Navbar variant='dark' bg='dark' sticky='top'>
  <Navbar.Brand>
    <User style={{marginLeft: 40}}
      src={loginWith.current === "GitHub" ? userDataGithub?.avatar_url : userDataGoogle?.picture}
      name={loginWith.current === "GitHub" ? userDataGithub?.login : userDataGoogle?.name}
      description={loginWith.current === "GitHub" ? userDataGithub?.bio : userDataGoogle?.email}
    />
  </Navbar.Brand>
  <Navbar.Collapse className='justify-content-end'>
    <Button style={{marginRight: 40}}
      className='ml-2'
      onClick={() => setLogOut()}
    >
      <LogOutIcon />
      Log out
    </Button>
  </Navbar.Collapse>
</Navbar>
{ <div className="d-flex flex-row">
  <div className="flex-grow-1">
    <div className="mt-3 p-3">
      <h2>Image Generation</h2>
      <form onSubmit={handleSubmit} className="mb-3">
        <div className="mb-3">
          <label htmlFor="prompt" className="form-label mr-3">Prompt:</label>
          <input type="text" className="form-control" id="prompt" value={prompt} onChange={handlePromptChange} />
        </div>
        <button type="submit" className="btn btn-primary mb-3">Generate image</button>
      </form>
	  <h2>Result Image Generation</h2>
	  {loading && <Spinner animation="border" variant="dark" />}
      <div className="d-flex flex-wrap justify-content-center">
        { images && (
          <img key={images} className="mr-3 mb-3" width={450} height={450} src={images} style={{ maxWidth: "100%" }} />
        )}
      </div>
    </div>
  </div>
  <div className="flex-grow-1">
    <div className="mt-3 p-3" style={{ backgroundColor: "white" }}>
      <h2>Edit Image</h2>
      <form onSubmit={handleImageSubmit} className="mt-3">
        <div className="mb-3">
          <label htmlFor="prompt2" className="form-label mr-3">Prompt:</label>
          <input type="text" className="form-control" id="prompt2" value={prompt2} onChange={handlePromptChange2} />
        </div>
        <div className="mb-3">
          <label htmlFor="image" className="form-label mr-3">Upload Image:</label>
          <input type="file" className="form-control-file" id="image" onChange={handleImageChange} />
        </div>
        {image && (
          <img
            src={URL.createObjectURL(image)}
            alt="Input image"
            width={450} height={450}
            className="mb-3"
            style={{ maxWidth: "100%"}}
          />
        )}
        <button type="submit"  style={{ maxWidth: "100%", marginTop: 50}} className="btn btn-primary mb-3">Update Image</button>
      </form>
	  <h2>Result Image Editing</h2>
	  {loading2 && <Spinner animation="border" variant="dark" />}
      {generatedImage && (
        <img
          width={450} height={450}
          src={generatedImage}
          alt="Generated image"
		  className="mb-3"
          style={{ maxWidth: "100%" }}
        />
      )}
    </div>
  </div>
</div> }
		</>
	)
}

export default Home
