package de.julianweinelt.caesar.worker.check;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

public class CompaCheck {
    public static boolean checkCompa() {
        GLFW.glfwInit();
        long window = GLFW.glfwCreateWindow(100, 100, "Check", 0, 0);
        GLFW.glfwMakeContextCurrent(window);
        GL.createCapabilities();

        String version = GL11.glGetString(GL11.GL_VERSION);
        System.out.println("OpenGL Version: " + version);

        boolean supportsGLES2 = version.contains("OpenGL ES 2.0") || isVersionAtLeast(version, 2, 0);
        System.out.println("Supports OpenGL ES 2: " + supportsGLES2);

        GLFW.glfwDestroyWindow(window);
        GLFW.glfwTerminate();
        return supportsGLES2;
    }

    private static boolean isVersionAtLeast(String versionString, int major, int minor) {
        try {
            String[] parts = versionString.split(" ")[0].split("\\.");
            int foundMajor = Integer.parseInt(parts[0]);
            int foundMinor = Integer.parseInt(parts[1]);
            return (foundMajor > major) || (foundMajor == major && foundMinor >= minor);
        } catch (Exception e) {
            return false;
        }
    }
}
