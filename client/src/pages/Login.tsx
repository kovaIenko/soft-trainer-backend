// import React, { useState } from "react";
// import { logo } from "../assets/icons";
// import toast, { Toaster } from "react-hot-toast";
import { useGoogleLogin } from "@react-oauth/google"
import { Card, Spacer, Button, Text, Container } from "@nextui-org/react"

import { IconGoogle } from "../assets/icons"
import { useNavigate } from "react-router-dom";

export type AuthPropType = {
  handleAuth: (value: any) => void;
};

export default function Login({ handleAuth }: AuthPropType) {
  const navigate = useNavigate()

	const loginToGoogle = useGoogleLogin({
		onSuccess: tokenResponse => {
			localStorage.setItem("loginWith", "Google")
			// console.log(tokenResponse)

			localStorage.setItem("accessToken", tokenResponse.access_token)
      handleAuth(tokenResponse.access_token);
			navigate("/chat")
		},
	})

  return (
    <>
      {/* <Toaster /> */}

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

      {/* <div
        style={{
          width: "100%",
          height: "100vh",
          backgroundColor: "white",
          display: "flex",
          justifyContent: "center",
        }}
      >
        <div
          className="box_auth"
          style={{
            color: "black",
            display: "flex",
            flexDirection: "column",
            alignItems: "center",
            marginTop: "24vh",
          }}
        >
          <p style={{ position: "absolute", top: "20px", fontSize: "28px" }}>
            {logo}
          </p>
          <h1 style={{ color: "black", marginBottom: "18px" }}>Welcome back</h1>
          <input
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            style={{
              marginTop: "15px",
              width: "270px",
              height: "41px",
              paddingLeft: "8px",
              fontSize: "16px",
              outline: "none",
              border: "1px solid gray",
              marginBottom: "18px",
              borderRadius: "4px",
            }}
            type="text"
            placeholder="Username"
          />
          <input
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            style={{
              marginTop: "15px",
              width: "270px",
              height: "41px",
              paddingLeft: "8px",
              fontSize: "16px",
              outline: "none",
              border: "1px solid gray",
              borderRadius: "4px",
              marginBottom: "18px",
            }}
            type="password"
            placeholder="Password"
          />
          <button
            onClick={() => handleLogin()}
            style={{
              marginTop: "15px",
              width: "285px",
              height: "41px",
              fontSize: "16px",
              outline: "none",
              border: "none",
              borderRadius: "6px",
              backgroundColor: "#10a37f",
              color: "white",
              marginBottom: "12px",
              cursor: "pointer",
            }}
          >
            Login
          </button>
          <p style={{ marginTop: "14px", fontSize: "14px" }}>
            Don't have an account?{" "}
            <Link
              style={{ color: "#10a37f", textDecoration: "none" }}
              to="/signup"
            >
              Sign up
            </Link>
          </p>
        </div>
      </div> */}
    </>
  );
}
