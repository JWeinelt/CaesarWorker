package de.julianweinelt.caesar.worker.check;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class FirstStartWindow {
    private static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public static void main(String[] args) {
        start();
    }

    public static void start() {
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setTitle("Caesar Worker - Caesar");
        frame.setResizable(false);
        frame.setLayout(new FlowLayout(FlowLayout.LEFT));

        JLabel title = new JLabel("Welcome to Caesar!");
        title.setFont(new Font(title.getFont().getFontName(), Font.BOLD, 30));
        frame.add(title);

        JPanel lineBreak = new JPanel();
        lineBreak.setPreferredSize(new Dimension(frame.getWidth(), 1));
        lineBreak.setOpaque(false);
        frame.add(lineBreak);

        JLabel info = new JLabel("We are preparing everything for you...");

        frame.add(info);

        frame.setVisible(true);

        scheduler.schedule(() -> {
            info.setText("Checking compatibility...");
        }, 3, TimeUnit.SECONDS);
    }


}