package model;

public class Question {

    private final int number;
    private final String question;
    private final String pathImage;
    private int currentAnswer;

    public Question(int number, String question, String pathImage) {
        this.number = number;
        this.question = question;
        this.pathImage = pathImage;
        currentAnswer = -1;
    }

    public int getNumber() {
        return number;
    }

    public String getQuestion() {
        return question;
    }

    public String getPathImage() {
        return pathImage;
    }

    public int getCurrentAnswer() {
        return currentAnswer;
    }

    public void setCurrentAnswer(int currentAnswer) {
        this.currentAnswer = currentAnswer;
    }
}
