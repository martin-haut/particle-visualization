package particleVisualization.model;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL32;
import particleVisualization.control.InputManager;
import particleVisualization.enums.HudDebugKeys;
import particleVisualization.enums.RenderMode;
import particleVisualization.enums.ShaderLayout;
import particleVisualization.enums.UniformName;
import particleVisualization.rendering.HeadUpDisplay;
import particleVisualization.rendering.Scene;
import particleVisualization.rendering.Shader;
import particleVisualization.rendering.Texture;
import particleVisualization.util.MiscUtils;
import particleVisualization.util.VertexSorter;


public class ParticleFieldSpeedLines extends DrawableEntity {

	private FloatBuffer			speedLineBuffer;
	private FloatBuffer			lineStripOffsets;
	private final IntBuffer		startingIndicesList;
	private final IntBuffer		numberOfverticesList;
	private final ParticleField	particleField;
	private final boolean		jumpCompensation		= true;
	private final float			filterKernel			= 0.5f;
	private float				speedlineTransparency	= 0.3f;
	private float				textureFact				= 0.5f;

	public ParticleFieldSpeedLines(ParticleField particleField, Texture texture) {
		super(RenderMode.globalColored, texture);
		this.particleField = particleField;
		startingIndicesList = BufferUtils.createIntBuffer(particleField.particlesPerFrame * 2);
		numberOfverticesList = BufferUtils.createIntBuffer(particleField.particlesPerFrame * 2);
		linkModelMatrix(particleField.getUpdatedModelMatrix());
	}

	@Override
	public void update() {
		if (InputManager.isKeyDown(GLFW.GLFW_KEY_2) && InputManager.isKeyDownEvent(GLFW.GLFW_KEY_PAGE_UP)) {
			speedlineTransparency = MiscUtils.clamp(speedlineTransparency + 0.1f, 0, 1);
		}
		if (InputManager.isKeyDown(GLFW.GLFW_KEY_2) && InputManager.isKeyDownEvent(GLFW.GLFW_KEY_PAGE_DOWN)) {
			speedlineTransparency = MiscUtils.clamp(speedlineTransparency - 0.1f, 0, 1);
		}
		if (InputManager.isKeyDown(GLFW.GLFW_KEY_1) && InputManager.isKeyDownEvent(GLFW.GLFW_KEY_PAGE_UP)) {
			textureFact = MiscUtils.clamp(textureFact + 0.1f, 0, 1);
		}
		if (InputManager.isKeyDown(GLFW.GLFW_KEY_1) && InputManager.isKeyDownEvent(GLFW.GLFW_KEY_PAGE_DOWN)) {
			textureFact = MiscUtils.clamp(textureFact - 0.1f, 0, 1);
		}
		HeadUpDisplay.putDebugValue(HudDebugKeys.speedlineTransparency, speedlineTransparency);
		HeadUpDisplay.putDebugValue(HudDebugKeys.textureFact, textureFact);

		int offsetCount = particleField.particlesPerFrame * (particleField.speedLineLength + 1) + particleField.particlesPerFrame;
		//		System.out.println("FRAME_LAYOUT: " + MiscUtils.vertexLayoutToString(dataFrames.get(0), 3, 3));
		if (lineStripOffsets == null || lineStripOffsets.capacity() != offsetCount) {
			lineStripOffsets = BufferUtils.createFloatBuffer(offsetCount);
		}
		speedLineBuffer = VertexSorter.frameLayoutToSpeedlineLayout(particleField.dataFrames, particleField.currentFrameIndex,
				particleField.speedLineLength + 1, 0, particleField.maxParticlesDisplayed, speedLineBuffer, lineStripOffsets, startingIndicesList,
				numberOfverticesList, jumpCompensation, filterKernel, particleField.getJumpThresholds());
		//		System.out.println("SLINE_LAYOUT: " + MiscUtils.vertexLayoutToString(speedLineBuffer, 3, 3) + "  speedLineLength:" + speedLineLength);
		//		System.out.println("startingIndicesArray: " + startingIndicesList.toString());
	}

	@Override
	protected void setPerDrawUniforms(Shader shader) {
		shader.setUniform1f(UniformName.spriteSize, particleField.globalRadius * 2f);
		shader.setUniform4f(UniformName.fogColor, Scene.BG_COLOR);
		shader.setUniform1f(UniformName.fogDensity, Scene.FOG_DENSITY);
		shader.setUniform1f(UniformName.speedlineTransparency, speedlineTransparency);
		shader.setUniform1f(UniformName.textureFact, textureFact);
	}

	@Override
	protected void drawVao(Shader shader, float startFraction, float countFraction) {
		if (particleField.speedLineLength > 0) {

			int vboId = glGenBuffers();
			glBindBuffer(GL_ARRAY_BUFFER, vboId);
			glBufferData(GL_ARRAY_BUFFER, speedLineBuffer, GL_STREAM_DRAW);
			glVertexAttribPointer(ShaderLayout.in_Position.ordinal(), 3, GL_FLOAT, false, 0, 0);
			glEnableVertexAttribArray(ShaderLayout.in_Position.ordinal());
			glBindBuffer(GL_ARRAY_BUFFER, 0);

			int oVboId = glGenBuffers();
			glBindBuffer(GL_ARRAY_BUFFER, oVboId);
			glBufferData(GL_ARRAY_BUFFER, lineStripOffsets, GL_STREAM_DRAW);
			glVertexAttribPointer(ShaderLayout.in_Offset.ordinal(), 1, GL_FLOAT, false, 0, 0);
			glEnableVertexAttribArray(ShaderLayout.in_Offset.ordinal());
			glBindBuffer(GL_ARRAY_BUFFER, 0);

			startingIndicesList.position((int) (particleField.maxParticlesDisplayed * startFraction));
			numberOfverticesList.position((int) (particleField.maxParticlesDisplayed * startFraction));

			startingIndicesList.limit((int) (particleField.maxParticlesDisplayed * countFraction));
			numberOfverticesList.limit((int) (particleField.maxParticlesDisplayed * countFraction));


			GL14.glMultiDrawArrays(GL32.GL_LINE_STRIP_ADJACENCY, startingIndicesList, numberOfverticesList);

			glDeleteBuffers(vboId);
			glDeleteBuffers(oVboId);
		}
	}



}
