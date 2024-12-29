import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class QuizPlatform {
    static final String DB_URL = "jdbc:mysql://localhost:3306/quiz_platform";
    static final String DB_USER = "root";
    static final String DB_PASSWORD = "";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginFrame());
    }
}

class LoginFrame extends JFrame {
    public LoginFrame() {
        setTitle("Quiz Platform - Login");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridLayout(3, 2));

        JLabel lblUsername = new JLabel("Username:");
        JTextField txtUsername = new JTextField();
        JLabel lblPassword = new JLabel("Password:");
        JPasswordField txtPassword = new JPasswordField();

        JButton btnLogin = new JButton("Login");
        JButton btnRegister = new JButton("Register");

        add(lblUsername);
        add(txtUsername);
        add(lblPassword);
        add(txtPassword);
        add(btnLogin);
        add(btnRegister);

        btnLogin.addActionListener(e -> {
            String username = txtUsername.getText();
            String password = new String(txtPassword.getPassword());
            if (authenticateUser(username, password)) {
                JOptionPane.showMessageDialog(this, "Login Successful!");
                new QuizSelectionFrame(username);
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Invalid credentials.");
            }
        });

        btnRegister.addActionListener(e -> {
            String username = txtUsername.getText();
            String password = new String(txtPassword.getPassword());
            if (registerUser(username, password)) {
                JOptionPane.showMessageDialog(this, "Registration Successful! Please login.");
            } else {
                JOptionPane.showMessageDialog(this, "Registration Failed.");
            }
        });

        setLocationRelativeTo(null);
        setVisible(true);
    }

    private boolean authenticateUser(String username, String password) {
        String query = "SELECT * FROM users WHERE username=? AND password=?";
        return executeDatabaseQuery(query, username, password);
    }

    private boolean registerUser(String username, String password) {
        String query = "INSERT INTO users (username, password) VALUES (?, ?)";
        return executeDatabaseUpdate(query, username, password);
    }

    private boolean executeDatabaseQuery(String query, String username, String password) {
        try (Connection connection = DriverManager.getConnection(QuizPlatform.DB_URL, QuizPlatform.DB_USER, QuizPlatform.DB_PASSWORD);
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    private boolean executeDatabaseUpdate(String query, String username, String password) {
        try (Connection connection = DriverManager.getConnection(QuizPlatform.DB_URL, QuizPlatform.DB_USER, QuizPlatform.DB_PASSWORD);
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            stmt.executeUpdate();
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }
}

class QuizSelectionFrame extends JFrame {
    public QuizSelectionFrame(String username) {
        setTitle("Quiz Platform - Select Category");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JLabel lblWelcome = new JLabel("Welcome, " + username);
        lblWelcome.setHorizontalAlignment(SwingConstants.CENTER);

        JButton btnMath = new JButton("Maths");
        JButton btnScience = new JButton("Java");
        JButton btnHistory = new JButton("Python");

        setLayout(new GridLayout(4, 1));
        add(lblWelcome);
        add(btnMath);
        add(btnScience);
        add(btnHistory);

        btnMath.addActionListener(e -> new QuizFrame("Maths", username));
        btnScience.addActionListener(e -> new QuizFrame("Java", username));
        btnHistory.addActionListener(e -> new QuizFrame("Python", username));

        setLocationRelativeTo(null);
        setVisible(true);
    }
}

class QuizFrame extends JFrame {
    private final String category;
    private final String username;
    private final List<Question> questions;
    private int currentQuestionIndex;
    private int score;
    private int timeLeft;
    private Timer timer;

    public QuizFrame(String category, String username) {
        this.category = category;
        this.username = username;
        this.questions = fetchQuestions(category);
        this.currentQuestionIndex = 0;
        this.score = 0;
        this.timeLeft = 15;

        setTitle("Quiz Platform - " + category);
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        showQuestion();

        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void showQuestion() {
        if (currentQuestionIndex >= questions.size()) {
            endQuiz();
            return;
        }

        Question question = questions.get(currentQuestionIndex);
        JPanel questionPanel = new JPanel(); // Normal JPanel for questions
        questionPanel.setLayout(new BorderLayout());

        JLabel lblQuestion = new JLabel((currentQuestionIndex + 1) + ". " + question.getText());
        lblQuestion.setHorizontalAlignment(SwingConstants.CENTER);
        questionPanel.add(lblQuestion, BorderLayout.NORTH);

        JLabel lblTimer = new JLabel("Time Left: " + timeLeft + "s");
        lblTimer.setHorizontalAlignment(SwingConstants.CENTER);
        questionPanel.add(lblTimer, BorderLayout.SOUTH);

        JPanel optionsPanel = new JPanel(new GridLayout(0, 1));
        ButtonGroup group = new ButtonGroup();
        List<JRadioButton> optionButtons = new ArrayList<>();

        for (int i = 0; i < question.getOptions().length; i++) {
            JRadioButton optionButton = new JRadioButton(question.getOptions()[i]);
            group.add(optionButton);
            optionButtons.add(optionButton);
            optionsPanel.add(optionButton);

            int optionIndex = i;
            optionButton.addActionListener(e -> {
                if (timer != null) {
                    timer.stop();
                }

                boolean isCorrect = optionIndex == question.getCorrectOption();
                String feedbackMessage = isCorrect ? "Correct!" : "Incorrect! The correct answer is: " + question.getOptions()[question.getCorrectOption()];
                JOptionPane.showMessageDialog(this, feedbackMessage);

                if (isCorrect) {
                    score++;
                }

                for (JRadioButton btn : optionButtons) {
                    btn.setEnabled(false);
                }

                SwingUtilities.invokeLater(() -> {
                    currentQuestionIndex++;
                    showQuestion();
                });
            });
        }

        questionPanel.add(optionsPanel, BorderLayout.CENTER);
        getContentPane().removeAll();
        add(questionPanel);
        revalidate();
        repaint();

        startTimer(lblTimer);
    }

    private void startTimer(JLabel lblTimer) {
        timeLeft = 15;
        lblTimer.setText("Time Left: " + timeLeft + "s");

        timer = new Timer(1000, e -> {
            timeLeft--;
            lblTimer.setText("Time Left: " + timeLeft + "s");

            if (timeLeft <= 5) {
                lblTimer.setForeground(Color.RED);
            }

            if (timeLeft == 0) {
                timer.stop();
                SwingUtilities.invokeLater(() -> {
                    currentQuestionIndex++;
                    showQuestion();
                });
            }
        });
        timer.start();
    }

    private void endQuiz() {
        JOptionPane.showMessageDialog(this, "Quiz Completed!\nScore: " + score + "/" + questions.size());
        new QuizSelectionFrame(username);
        dispose();
    }

    private List<Question> fetchQuestions(String category) {
        List<Question> questionList = new ArrayList<>();
        String query = "SELECT * FROM questions WHERE category = ?";
        try (Connection connection = DriverManager.getConnection(QuizPlatform.DB_URL, QuizPlatform.DB_USER, QuizPlatform.DB_PASSWORD);
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, category);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String text = rs.getString("text");
                String[] options = new String[] {
                        rs.getString("option1"),
                        rs.getString("option2"),
                        
                };
                int correctOption = rs.getInt("correct_option");
                questionList.add(new Question(text, options, correctOption));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return questionList;
    }
}

class Question {
    private final String text;
    private final String[] options;
    private final int correctOption;

    public Question(String text, String[] options, int correctOption) {
        this.text = text;
        this.options = options;
        this.correctOption = correctOption;
    }

    public String getText() {
        return text;
    }

    public String[] getOptions() {
        return options;
    }

    public int getCorrectOption() {
        return correctOption;
    }
}
