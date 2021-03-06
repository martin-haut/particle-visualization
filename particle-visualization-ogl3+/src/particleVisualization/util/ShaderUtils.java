package particleVisualization.util;

import static org.lwjgl.opengl.GL20.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import org.lwjgl.opengl.GL32;
import particleVisualization.enums.ShaderLayout;


public class ShaderUtils {

	private static final String	VERTEX_SHADER_FOLDER	= "src/shaders/vertex/";
	private static final String	GEOMETRY_SHADER_FOLDER	= "src/shaders/geometry/";
	private static final String	FRAGMENT_SHADER_FOLDER	= "src/shaders/fragment/";


	public static int buildShader(String vertexShaderName, String fragmentShaderName) {
		return buildShader(vertexShaderName, null, fragmentShaderName);
	}

	public static int buildShader(String vertexShaderName, String geometryShaderName, String fragmentShaderName) {
		int vsId = compileShaderFromFile(VERTEX_SHADER_FOLDER + vertexShaderName, GL_VERTEX_SHADER);
		int gsId = -1;
		if (geometryShaderName != null) {
			gsId = compileShaderFromFile(GEOMETRY_SHADER_FOLDER + geometryShaderName, GL32.GL_GEOMETRY_SHADER);
		}
		int fsId = compileShaderFromFile(FRAGMENT_SHADER_FOLDER + fragmentShaderName, GL_FRAGMENT_SHADER);

		int shaderProgramId = glCreateProgram();
		glAttachShader(shaderProgramId, vsId);
		if (geometryShaderName != null) {
			glAttachShader(shaderProgramId, gsId);
		}
		glAttachShader(shaderProgramId, fsId);

		// set global locations for attribs
		for (ShaderLayout varName: ShaderLayout.values()) {
			glBindAttribLocation(shaderProgramId, varName.ordinal(), varName.toString());
		}

		glLinkProgram(shaderProgramId);
		String log = glGetProgramInfoLog(shaderProgramId, GL_LINK_STATUS);
		if (!log.equals("")) {
			System.err.println("Shader linking error:\n" + log);
		}
		glValidateProgram(shaderProgramId);

		return shaderProgramId;
	}


	public static int compileShaderFromFile(final String filename, final int type) {
		StringBuilder shaderSource = new StringBuilder();
		int shaderID = 0;

		try {
			BufferedReader reader = new BufferedReader(new FileReader(filename));
			String line;
			while ((line = reader.readLine()) != null) {
				shaderSource.append(line).append("\n");
			}
			reader.close();
		}
		catch (IOException e) {
			System.err.println("Could not read file: " + filename);
			e.printStackTrace();
			System.exit(-1);
		}

		shaderID = glCreateShader(type);
		glShaderSource(shaderID, shaderSource);
		glCompileShader(shaderID);
		String log = glGetShaderInfoLog(shaderID, 1000);
		if (!log.equals("")) throw new RuntimeException("Shader compile error:\n" + log);

		return shaderID;
	}



}
