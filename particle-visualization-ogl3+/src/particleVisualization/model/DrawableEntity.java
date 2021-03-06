package particleVisualization.model;

import static org.lwjgl.opengl.GL11.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import particleVisualization.enums.RenderMode;
import particleVisualization.rendering.Shader;
import particleVisualization.rendering.Texture;
import particleVisualization.rendering.VertexArrayObject;
import particleVisualization.util.MiscUtils;


public abstract class DrawableEntity extends Entity {


	private final Vector3f				modelScale			= new Vector3f(1, 1, 1);
	private Matrix4f					modelMatrix			= new Matrix4f();

	//	private float[] 		vertices;
	protected final VertexArrayObject	vertexArrayObject;
	private VertexArrayObject			bBoxVertexArrayObject;
	private final Texture				texture;
	private RenderMode					renderMode;

	private Vector3f					bBoxMin, bBoxMax, bBoxMid;
	private boolean						drawBoundingBox		= false;
	private boolean						modelMatrixLinked	= false;



	/**
	 * primitiveMode = symbolic constant GL_POINTS, GL_LINE_STRIP, GL_LINE_LOOP, GL_LINES, GL_LINE_STRIP_ADJACENCY, GL_LINES_ADJACENCY, GL_TRIANGLE_STRIP,
	 * GL_TRIANGLE_FAN, GL_TRIANGLES, GL_TRIANGLE_STRIP_ADJACENCY, GL_TRIANGLES_ADJACENCY or GL_PATCHES
	 */
	public DrawableEntity(Texture texture, float[] positions, byte[] indices, float[] texCoords, int primitiveMode) {
		this.texture = texture;
		renderMode = RenderMode.textured;
		//		this.vertices = positions;
		vertexArrayObject = new VertexArrayObject(positions, indices, texCoords, primitiveMode);
	}

	public DrawableEntity(Texture texture, float[] initialPositions, float[] initialColors, int verticesTargetCount, int primitiveMode, RenderMode renderMode) {
		this.texture = texture;
		this.renderMode = renderMode;
		//		this.vertices = initialPositions;
		vertexArrayObject = new VertexArrayObject(initialPositions, initialColors, primitiveMode, verticesTargetCount);
	}

	public DrawableEntity(RenderMode renderMode, Texture texture) {
		this.renderMode = renderMode;
		vertexArrayObject = null;
		this.texture = texture;
	};


	protected abstract void setPerDrawUniforms(Shader shader);

	/**
	 * e.g.: vertexArrayObject.draw();
	 *
	 * @param shader
	 * @param countFraction
	 * @param startFraction
	 */
	protected abstract void drawVao(Shader shader, float startFraction, float countFraction);



	public final void draw(Shader shader, float startFraction, float countFraction) {
		if (texture != null) {
			texture.bind();
		}
		shader.setModelMatrix(getUpdatedModelMatrix());
		shader.setRenderMode(renderMode);
		setPerDrawUniforms(shader);
		drawVao(shader, startFraction, countFraction);
		if (drawBoundingBox && startFraction == 0f) {
			shader.setRenderMode(RenderMode.boundingBox);
			glDisable(GL_CULL_FACE);
			GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);
			getBBoxVertexArrayObject().draw();
			GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
			glEnable(GL_CULL_FACE);
		}
		if (texture != null) {
			texture.unbind();
		}
	}


	private VertexArrayObject getBBoxVertexArrayObject() {
		if (bBoxVertexArrayObject == null) {
			bBoxVertexArrayObject = new VertexArrayObject(MiscUtils.cornerVectorsToQuadstrip(getBoundingBoxMin(), getBoundingBoxMax()), null, null, GL11.GL_QUAD_STRIP);
		}
		return bBoxVertexArrayObject;
	}

	public Vector3f getBoundingBoxMin() {
		if (bBoxMin == null) {
			calcBoundingBox();
		}
		return bBoxMin;
	}

	public Vector3f getBoundingBoxMax() {
		if (bBoxMax == null) {
			calcBoundingBox();
		}
		return bBoxMax;
	}

	public Vector3f getBoundingBoxMid() {
		if (bBoxMid == null) {
			bBoxMid = Vector3f.sub(getBoundingBoxMax(), getBoundingBoxMin(), null);
			bBoxMid.scale(0.5f);
			Vector3f.add(bBoxMid, getBoundingBoxMin(), bBoxMid);
		}
		return bBoxMid;
	}

	public void setBoundingBoxMin(Vector3f bBoxMin) {
		this.bBoxMin = bBoxMin;
	}

	public void setBoundingBoxMax(Vector3f bBoxMax) {
		this.bBoxMax = bBoxMax;
	}

	private void calcBoundingBox() {
		// TODO calc BoundingBox from vertices if not set
	}


	public void addScaleClipped(float x, float y, float z) {
		modelScale.x = MiscUtils.clamp(modelScale.x + x, .0001f, 110f);
		modelScale.y = MiscUtils.clamp(modelScale.y + y, .0001f, 110f);
		modelScale.z = MiscUtils.clamp(modelScale.z + z, .0001f, 110f);
		needsMatrixUpdate = true;
	}

	public void scaleClipped(float s) {
		modelScale.x = MiscUtils.clamp(modelScale.x * s, .0001f, 110f);
		modelScale.y = MiscUtils.clamp(modelScale.y * s, .0001f, 110f);
		modelScale.z = MiscUtils.clamp(modelScale.z * s, .0001f, 110f);
		needsMatrixUpdate = true;
	}

	public void scaleClippedX(float s) {
		modelScale.x = MiscUtils.clamp(modelScale.x * s, .0001f, 110f);
		needsMatrixUpdate = true;
	}

	public void addScale(float s) {
		addScaleClipped(s, s, s);
	}

	public void setScale(float s) {
		modelScale.x = s;
		modelScale.y = s;
		modelScale.z = s;
		needsMatrixUpdate = true;
	}

	private void updateModelMatrix() {
		modelMatrix.setIdentity();
		Matrix4f.translate(getPosition(), modelMatrix, modelMatrix);
		Matrix4f.scale(modelScale, modelMatrix, modelMatrix);
		Matrix4f.rotate(MiscUtils.degreesToRadians(getYaw()), UNIT_VECTOR_Y, modelMatrix, modelMatrix);
		Matrix4f.rotate(MiscUtils.degreesToRadians(getPitch()), UNIT_VECTOR_X, modelMatrix, modelMatrix);
		Matrix4f.rotate(MiscUtils.degreesToRadians(getRoll()), UNIT_VECTOR_Z, modelMatrix, modelMatrix);
		if (bBoxMin != null) {
			Matrix4f.translate(getBoundingBoxMid().negate(null), modelMatrix, modelMatrix);
		}
	}

	public Matrix4f getUpdatedModelMatrix() {
		if (needsMatrixUpdate && !modelMatrixLinked) {
			updateModelMatrix();
			needsMatrixUpdate = false;
		}
		return modelMatrix;
	}

	public void destroy() {
		if (texture != null) {
			texture.destroy();
		}
		if (vertexArrayObject != null) {
			vertexArrayObject.destroy();
		}
	}

	public boolean drawBoundingBox() {
		return drawBoundingBox;
	}

	public void drawBoundingBox(boolean drawBoundingBox) {
		this.drawBoundingBox = drawBoundingBox;
	}

	public void toggleBoundingBox() {
		drawBoundingBox = !drawBoundingBox;
	}


	public RenderMode getRenderMode() {
		return renderMode;
	}

	public void setRenderMode(RenderMode renderMode) {
		this.renderMode = renderMode;
	}


	public void linkModelMatrix(Matrix4f modelMatrix) {
		modelMatrixLinked = true;
		this.modelMatrix = modelMatrix;
	}



}
