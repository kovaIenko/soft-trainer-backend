import {OnboardingQuestionType} from "./OnboardingQuestionType";

interface Question {
    question: string;
    type: OnboardingQuestionType;
    options?: string[];
}

export default Question