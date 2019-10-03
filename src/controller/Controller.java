package controller;

import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.Timer;
import model.Question;
import view.GuiQuestions;
import view.GuiMessage;
import view.Login;

public class Controller {

    private final static String LOCAL = "";
    private final static String ANSWERS = "";
    protected final static String PROP = LOCAL + "resources/pathTests.properties";

    private GuiQuestions guiQuestions = null;
    private GuiMessage guiMessage = null;
    private Login guiLogin = null;

    private ArrayList<Question> currentListQuestions;
    private ArrayList<String> groups;
    private String pathQuestions;
    private String pathAnswers;
    private String pathGroups;
    private String pathImages;
    private String pathImagesBtn;

    private String currentUser;
    private String currentGroup;
    private int currentQuestion;
    private int countTime;
    private boolean openQuestions;
    private boolean allAnswered;
    private final Timer timer;
    private final int LIMIT = 15 * 60;

    public Controller() {

        // checar arquivos
        checkBaseFiles();

        // GUI Login
        guiLogin = new Login();
        guiLogin.eventLogin(new ActionsGuiLogin());
        guiLogin.setAlwaysOnTop(true);
        guiLogin.setResizable(false);
        guiLogin.lblName.setText("Por favor, escolha uma opção e clique em Responder!");

        // GUI Perguntas
        guiQuestions = new GuiQuestions();
        guiQuestions.eventTest(new ActionsGuiTest());
        guiQuestions.setAlwaysOnTop(false);
        guiQuestions.setResizable(false);
        guiQuestions.setPreferredSize(new Dimension(900, 600));
        guiQuestions.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent evt) {
                System.out.println("pause application ...");
                if (inform("Deseja encerrar?", "Fechar Pesquisa")) {
                    save(formatAnswers(), "-closed.csv");
                    login();
                } else {
                    System.out.println("continue ...");
                }
            }
        });

        // inserir imagens nos botões da GUI Perguntas
        updateImageButtons(guiQuestions.btn1, "btn_0.png");
        updateImageButtons(guiQuestions.btn2, "btn_1.png");
        updateImageButtons(guiQuestions.btn3, "btn_2.png");
        updateImageButtons(guiQuestions.btn4, "btn_3.png");
        updateImageButtons(guiQuestions.btn5, "btn_4.png");

        // GUI Opinião
        guiMessage = new GuiMessage();
        guiMessage.eventTest(new ActionsGuiText());
        guiMessage.setAlwaysOnTop(false);
        guiMessage.setResizable(false);

        // contagem de tempo
        openQuestions = false;
        countTime = 0;
        timer = new Timer(1000, (ActionEvent e) -> {
            if (openQuestions) {
                countTime++;
            }
            if (countTime >= LIMIT) {
                guiQuestions.txtAreaQuestion.setText("Aplicação aguardando fechamento!"
                        + "\nFavor fechar ou salvar respostas!");
            }
            if (countTime >= (LIMIT * 2)) {
                ((Timer) (e.getSource())).stop();
                login();
            }
        });

        // iniciar
        Util.export("", pathAnswers + Util.timeNow() + "-startLogin.txt");
        loadData();
        login();
    }

    private void checkBaseFiles() {
        System.out.println("open application ...");
        try {
            pathQuestions = LOCAL + Util.property(PROP, "PATHQUESTIONS");
            pathImages = LOCAL + Util.property(PROP, "PATHIMAGES");
            pathImagesBtn = LOCAL + Util.property(PROP, "PATHIMAGESBTN");
            pathGroups = LOCAL + Util.property(PROP, "PATHGROUPS");
            // respostas salvas em diretório individual
            pathAnswers = ANSWERS + Util.property(PROP, "PATHANSWER");
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
        }

        if (Util.createDirectory(pathAnswers)) {
            System.out.println(pathAnswers + " ok ...");
        } else {
            System.out.println("new directory ... " + pathAnswers + " ...");
        }
    }

    private void loadData() {
        groups = Util.loadFile(pathGroups);
        guiLogin.cboxGroups.removeAllItems();
        if (groups != null) {
            Collections.sort(groups);
            guiLogin.cboxGroups.addItem(" Visitante");
            groups.forEach((s) -> {
                guiLogin.cboxGroups.addItem(s);
            });
        }
    }

    private void login() {
        // nova rodada
        currentQuestion = 0;
        currentUser = "";
        currentGroup = "";
        openQuestions = false;
        allAnswered = false;
        countTime = 0;
        timer.setInitialDelay(0);
        timer.start();              // iniciar objeto Timer        
        guiLogin.cboxGroups.setSelectedIndex(0);
        guiLogin.setVisible(true);
        guiQuestions.setVisible(false);
        guiMessage.setVisible(false);
        guiQuestions.lblUser.setText("");
        guiMessage.txtAreaMessage.setText("");
    }

    private void closeLogin() {
        //currentUser = guiLogin.txtFieldName.getText();
        currentGroup = String.valueOf(guiLogin.cboxGroups.getSelectedItem());
        startTest(pathQuestions);
    }

    private void exitLogin() {
        System.out.println("close application ...");
        System.exit(0);
    }

    private void startTest(String file) {
        System.out.println("check questions ... " + file + "...");
        if (loadQuestions(file)) {
            // preparar GUI Perguntas
            guiLogin.setVisible(false);
            guiQuestions.setVisible(true);
            if (currentUser.equals("")) {
                guiQuestions.lblUser.setText(currentGroup);
            } else {
                guiQuestions.lblUser.setText(currentUser + " : " + currentGroup);
            }

            // ativar contador de tempo
            openQuestions = true;

            // atualizar questão
            updateQuestion();

        } else {
            if (inform("Houve algo de errado!\nNão há teste para:\n" + currentGroup
                    + "\nDeseja encerrar?", "Atenção!")) {
                exitLogin();
            }
        }
    }

    private boolean loadQuestions(String file) {
        System.out.println("load questions ...");
        boolean emptyQuestions = true;
        currentListQuestions = new ArrayList<>();
        ArrayList<String> questions = Util.loadFile(file);

        String[] str;
        int number = 1;
        for (int i = 0; i < questions.size(); i++) {
            str = questions.get(i).split(Util.DELIM);
            if (str.length == 2) {
                // Question(numero da questão, questao, pathImagem)          
                currentListQuestions.add(new Question(number++, str[1], str[0]));
                emptyQuestions = false;
            }
        }
        return !emptyQuestions;
    }

    private void updateQuestion() {
        System.out.println("counter: " + countTime + " ...");
        if (currentQuestion < 0) {
            currentQuestion = 0;
        }
        if (currentQuestion >= currentListQuestions.size() - 1) {
            currentQuestion = currentListQuestions.size() - 1;
        }

        // atualizar questão
        String str;
        if (currentQuestion == currentListQuestions.size() - 1) {
            str = "Última pergunta";
        } else {
            str = "Pergunta " + currentListQuestions.get(currentQuestion).getNumber()
                    + " de " + Integer.toString(currentListQuestions.size());
        }
        str += ":\n" + currentListQuestions.get(currentQuestion).getQuestion();

        int answer = currentListQuestions.get(currentQuestion).getCurrentAnswer();
        String[] nivel = {"Muito Ruim", "Ruim", "Bom", "Muito Bom", "Excelente"};
        if (answer != -1) {
            str += "\n\nResposta Escolhida:\n" + nivel[answer - 1];
        }

        guiQuestions.txtAreaQuestion.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        guiQuestions.txtAreaQuestion.setText(str);

        // atualizar imagem da questão
        updateImageQuestion();
    }

    private void updateSavedResponses(int option) {
        // salvar a resposta escolhida pelo usuário
        currentListQuestions.get(currentQuestion).setCurrentAnswer(option);
        updateQuestion();
        if (!allAnswered) {
            allAnswered = true;
            for (int i = 0; i < currentListQuestions.size(); i++) {
                if (currentListQuestions.get(i).getCurrentAnswer() == -1) {
                    allAnswered = false;
                    break;
                }
            }
        }
        if (allAnswered) {
            guiQuestions.lblUser.setText(currentGroup + ", tudo respondido."
                    + " Você já pode revisar ou Salvar.");
        } 
        System.out.println("current answer updated ...");
    }
    
    private void updateImageQuestion() {
        String imagePath = pathImages + currentListQuestions.get(currentQuestion).getPathImage();
        try {
            BufferedImage imgOriginal = ImageIO.read(new File(imagePath));
            Image img = imgOriginal.getScaledInstance(
                    guiQuestions.lblImage.getWidth(),
                    guiQuestions.lblImage.getHeight(),
                    Image.SCALE_SMOOTH);
            ImageIcon imgLabel = new ImageIcon(img);
            guiQuestions.lblImage.setText("");
            guiQuestions.lblImage.setIcon(imgLabel);
        } catch (IOException ex) {
            String str = "Questão sem imagem disponível!";
            guiQuestions.lblImage.setText(str);
            guiQuestions.lblImage.setIcon(null);
        }
    }

    private void updateImageButtons(JButton button, String image) {
        String imagePath = pathImagesBtn + image;
        try {
            BufferedImage imgOriginal = ImageIO.read(new File(imagePath));
            Image img = imgOriginal.getScaledInstance(
                    button.getWidth(),
                    button.getHeight(),
                    Image.SCALE_SMOOTH);
            ImageIcon imgBtn = new ImageIcon(img);
            button.setText("");
            button.setIcon(imgBtn);
            //button.setBorder(null);
            //button.setContentAreaFilled(false);

        } catch (IOException ex) {
            String str = "Imagem indisponível!";
            button.setText("");
            button.setIcon(null);
        }
    }

    private String formatAnswers() {
        String summary = "";
        for (int i = 0; i < currentListQuestions.size(); i++) {
            summary += currentListQuestions.get(i).getCurrentAnswer() + Util.DELIM;
            summary += currentListQuestions.get(i).getQuestion() + Util.DELIM;
            if (currentUser.equals("")) {
                summary += "anônimo" + Util.DELIM;
            } else {
                summary += currentUser + Util.DELIM;
            }
            summary += currentGroup + Util.DELIM;
            summary += Util.timeNow();
            summary += "\n";
        }
        return summary;
    }

    private void save(String summary, String extension) {

        // salvar respostas no arquivo de saída
        String path = pathAnswers;
        if (!currentUser.equals("")) {
            path += currentUser.replace(" ", "-") + "-";
        }
        path += currentGroup.replace(" ", "");
        path += "_" + Util.timeNow();
        path += extension;
        Util.export(summary, path);
    }

    private boolean inform(String mensagem, String titulo) {
        int dialogResult = JOptionPane.showConfirmDialog(guiQuestions,
                mensagem, titulo, JOptionPane.YES_NO_OPTION);
        return dialogResult == 0;

    }

    class ActionsGuiLogin implements ActionListener {

        // controle dos botões do GUI Login
        @Override
        public void actionPerformed(ActionEvent ev) {
            Object source = ev.getSource();
            if (source == guiLogin.btnLogin) {
                closeLogin();
            }
            if (source == guiLogin.btnQuit) {
                Util.export("", pathAnswers + Util.timeNow() + "-exitLogin.txt");
                exitLogin();
            }
        }
    }

    class ActionsGuiTest implements ActionListener {

        // controle dos botões do GUI Perguntas
        @Override
        public void actionPerformed(ActionEvent ev) {
            Object source = ev.getSource();
            if (source == guiQuestions.btn1) {
                if (currentListQuestions.get(currentQuestion).getCurrentAnswer() == -1) {
                    updateSavedResponses(1);
                    currentQuestion++;
                    updateQuestion();
                } else {
                    updateSavedResponses(1);
                }
                System.out.println("btn1 ...");
            }
            if (source == guiQuestions.btn2) {
                if (currentListQuestions.get(currentQuestion).getCurrentAnswer() == -1) {
                    updateSavedResponses(2);
                    currentQuestion++;
                    updateQuestion();
                } else {
                    updateSavedResponses(2);
                }
                System.out.println("btn2 ...");
            }
            if (source == guiQuestions.btn3) {
                if (currentListQuestions.get(currentQuestion).getCurrentAnswer() == -1) {
                    updateSavedResponses(3);
                    currentQuestion++;
                    updateQuestion();
                } else {
                    updateSavedResponses(3);
                }
                System.out.println("btn3 ...");
            }
            if (source == guiQuestions.btn4) {
                if (currentListQuestions.get(currentQuestion).getCurrentAnswer() == -1) {
                    updateSavedResponses(4);
                    currentQuestion++;
                    updateQuestion();
                } else {
                    updateSavedResponses(4);
                }
                System.out.println("btn4 ...");
            }
            if (source == guiQuestions.btn5) {
                if (currentListQuestions.get(currentQuestion).getCurrentAnswer() == -1) {
                    updateSavedResponses(5);
                    currentQuestion++;
                    updateQuestion();
                } else {
                    updateSavedResponses(5);
                }
                System.out.println("btn5 ...");
            }
            if (source == guiQuestions.btnFirst) {
                currentQuestion = 0;
                updateQuestion();
            }
            if (source == guiQuestions.btnPrevious) {
                currentQuestion--;
                updateQuestion();
            }
            if (source == guiQuestions.btnNext) {
                currentQuestion++;
                updateQuestion();
            }
            if (source == guiQuestions.btnLast) {
                currentQuestion = currentListQuestions.size() - 1;
                updateQuestion();
            }
            if (source == guiQuestions.btnFinish) {
                save(formatAnswers(), ".csv");
                if (inform("\n\nGostaria de deixar um recado?  "
                        + "\nSugestão, Crítica ou Opinião?\n\n",
                        "Só mais uma pergunta!")) {
                    guiQuestions.setVisible(false);
                    guiMessage.setVisible(true);
                } else {
                    login();
                }
            }
        }
    }

    class ActionsGuiText implements ActionListener {

        // controle dos botões do GUI Opinião
        @Override
        public void actionPerformed(ActionEvent ev) {
            Object source = ev.getSource();
            if (source == guiMessage.btnClean) {
                guiMessage.txtAreaMessage.setText("");
                System.out.println("opinião limpa ...");
            }
            if (source == guiMessage.btnClose) {
                String strOpinion = guiMessage.txtAreaMessage.getText();
                if (!strOpinion.equals("")) {
                    save(strOpinion, ".txt");
                }
                login();
            }
        }
    }
}
