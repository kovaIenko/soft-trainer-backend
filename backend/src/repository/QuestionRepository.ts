import {ChatType} from "../entity/ChatType";
import {getOnboardingQuestion} from "./OnboardingRepository";
import {postMessage as postMessageToGpt} from "./GptRepository";

export function getNextMessage(userId:string, chatType: ChatType, lastAnswer: string) {
    switch (chatType) {
        case ChatType.Onboarding:
            return getOnboardingQuestion(userId);
        default:
            return postMessageToGpt(lastAnswer);
    }
}