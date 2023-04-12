import { useGoogleLogin } from "@react-oauth/google"
import { useNavigate } from "react-router-dom"

import { Card, Spacer, Button, Text, Container } from "@nextui-org/react"

import { IconGitHub, IconGoogle } from "../../assets/icons"

const GITHUB_CLIENT_ID = import.meta.env.VITE_GITHUB_CLIENT_ID

const Login = () => {
	const navigate = useNavigate()

	const loginToGithub = () => {
		localStorage.setItem("loginWith", "GitHub")
		window.location.assign(`https://github.com/login/oauth/authorize?client_id=${GITHUB_CLIENT_ID}`)
	}

	const loginToGoogle = useGoogleLogin({
		onSuccess: tokenResponse => {
			localStorage.setItem("loginWith", "Google")
			console.log(tokenResponse)
			localStorage.setItem("accessToken", tokenResponse.access_token)
			navigate("/home")
		},
	})

	return (
		<Container display='flex' alignItems='center' justify='center' css={{ minHeight: "100vh" }}>
			<Card css={{ mw: "420px", p: "20px" }}>
				<Text
					size={24}
					weight='bold'
					css={{
						as: "center",
						mb: "20px",
					}}
				>
					Login with
				</Text>
				{/* <Spacer y={1} />
				<Button color='gradient' auto ghost onPress={() => loginToGithub()}>
					<IconGitHub />
					<Spacer x={0.5} />
					GitHub
				</Button>
				<Spacer y={1} /> */}

				<Button color='gradient' auto ghost onPress={() => loginToGoogle()}>
					<IconGoogle />
					<Spacer x={0.5} />
					Google
				</Button>
			</Card>
		</Container>
	)
}

export default Login
