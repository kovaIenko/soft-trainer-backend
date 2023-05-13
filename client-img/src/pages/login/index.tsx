import { useGoogleLogin } from "@react-oauth/google"
import { useNavigate } from "react-router-dom"

import { Card, Spacer, Button, Text, Container } from "@nextui-org/react"

import { IconGoogle } from "../../assets/icons"

const Login = () => {
	const navigate = useNavigate()

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