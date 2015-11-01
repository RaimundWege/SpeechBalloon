package de.raimundwege.speechballoon;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.CatmullRomSpline;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Path;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

public class SpeechBalloonUtils {

	public enum SpeechBalloonType {
		NORMAL, SCREAM, THINK, WHISPER
	}

	private final static Vector2 pos = new Vector2();
	private final static Vector2 dir = new Vector2();
	private final static Vector2 derivative = new Vector2();
	private final static Vector2 nor = new Vector2();

	private final static Vector3 dir3D = new Vector3();
	private final static Vector3 crs3D = new Vector3();
	private final static Vector3 up3D = new Vector3();

	private final static int POSITION = 3;
	private final static int COLOR = 4;

	public static ModelInstance createSpeechBalloon(
			SpeechBalloonParameters params) {
		Model model = modelForRectangle(params);
		return new ModelInstance(model);
	}

	private static Model modelForRectangle(SpeechBalloonParameters params) {
		Vector2[] controlPoints = {
				new Vector2(params.position),
				new Vector2(params.position.x + params.size.x,
						params.position.y),
				new Vector2(params.position.x + params.size.x,
						params.position.y + params.size.y),
				new Vector2(params.position.x, params.position.y
						+ params.size.y) };
		Path<Vector2> path = new CatmullRomSpline<Vector2>(controlPoints, true);
		return modelForPath(path, params);
	}

	private static Model modelForPath(Path<Vector2> path,
			SpeechBalloonParameters params) {
		Array<Vector2> innerRingPoints = new Array<Vector2>();
		Array<Vector2> styleRingPoints = new Array<Vector2>();
		Array<Vector2> outerRingPoints = new Array<Vector2>();

		// Progress
		float progress = 0f;
		float time = 0f;
		float pathLength = path.approxLength(params.pointCount);
		float unitsPerPoint = pathLength / params.pointCount;
		float averageDerivative = averageDerivativeForPath(path,
				params.pointCount);
		float distance = 0f;

		// Cloud style
		float cloudAngle = 0f;
		float cloudHeight = 0f;

		// Target
		float targetAngle = params.targetAngle();
		float currentTargetAngle = 0f;
		float minTargetAngle = Float.MAX_VALUE;
		int minTargetIndex = 0;

		// Control points
		for (int i = 0; i < params.pointCount; i++) {
			progress = (float) i / params.pointCount;

			path.valueAt(pos, time);
			path.derivativeAt(derivative, time);

			calculateNormal(nor, pos, derivative, params.origin());

			// Inner ring points
			innerRingPoints.add(pos.cpy());

			// Style ring points
			Vector2 stylePosition = new Vector2(pos);
			if (params.type == SpeechBalloonType.SCREAM) {
				stylePosition.add(nor.cpy().scl(i % 2 == 0 ? 2 : 10));
			} else if (params.type == SpeechBalloonType.THINK) {
				cloudAngle = (progress * 180 * params.cloudCount) % 180;
				cloudHeight = MathUtils.sinDeg(cloudAngle) * params.cloudHeight;
				stylePosition.add(nor.cpy().scl(cloudHeight));
			} else {
				stylePosition.add(nor.cpy().scl(2));
			}
			styleRingPoints.add(stylePosition);

			// Outer ring points
			Vector2 outerPosition = new Vector2(stylePosition);
			outerPosition.add(nor.cpy().scl(params.outlineWidth));
			outerRingPoints.add(outerPosition);

			// Target
			currentTargetAngle = Math.abs(nor.angle() - targetAngle);
			if (currentTargetAngle < minTargetAngle) {
				minTargetAngle = currentTargetAngle;
				minTargetIndex = i;
			}

			// Calcualte progress
			float difference = derivative.len() / averageDerivative;
			distance += unitsPerPoint / difference;
			time = distance / pathLength;
		}

		// Target
		if (params.type != SpeechBalloonType.THINK) {
			boolean removeStartIndex = false;
			int targetIndex = minTargetIndex;
			Vector2 innerStartVector = innerRingPoints.get(targetIndex);
			Vector2 innerEndVector = innerRingPoints.get(targetIndex);
			int targetPointCount = (int) (0.05 * params.pointCount);
			for (int i = 0; i < targetPointCount; i++) {
				removeStartIndex = (i % 2 == 0);
				targetIndex += (removeStartIndex ? -1 : 0);
				targetIndex = validateIndex(targetIndex, innerRingPoints.size);
				if (removeStartIndex) {
					innerStartVector = innerRingPoints.removeIndex(targetIndex);
				} else {
					innerEndVector = innerRingPoints.removeIndex(targetIndex);
				}
				styleRingPoints.removeIndex(targetIndex);
				outerRingPoints.removeIndex(targetIndex);
			}

			// Add target point
			dir.set(innerEndVector).sub(innerStartVector).scl(0.5f);
			pos.set(innerStartVector).add(dir);
			innerRingPoints.insert(targetIndex, pos.cpy());
			dir.set(pos).sub(params.target).scl(1f - params.targetScale);
			pos.set(params.target).add(dir);
			styleRingPoints.insert(targetIndex, pos.cpy());
			pos.set(params.target).add(dir.scl(0.8f));
			outerRingPoints.insert(targetIndex, pos.cpy());

			int beforeIndex = validateIndex(targetIndex - 1,
					styleRingPoints.size);
			int afterIndex = validateIndex(targetIndex + 1,
					styleRingPoints.size);

			pos.set(styleRingPoints.get(beforeIndex));
			dir.set(pos).add(styleRingPoints.get(targetIndex));
			calculateNormal(nor, pos, dir, innerRingPoints.get(targetIndex));
			nor.scl(params.outlineWidth);
			System.out.println(nor);
			outerRingPoints.get(beforeIndex)
					.set(styleRingPoints.get(beforeIndex)).add(nor);

			pos.set(styleRingPoints.get(afterIndex));
			dir.set(pos).add(styleRingPoints.get(targetIndex));
			calculateNormal(nor, pos, dir, innerRingPoints.get(targetIndex));
			nor.scl(params.outlineWidth);
			System.out.println(nor);
			outerRingPoints.get(afterIndex)
					.set(styleRingPoints.get(afterIndex)).add(nor);
		}

		// Model builder
		ModelBuilder modelBuilder = new ModelBuilder();
		modelBuilder.begin();

		// Mesh parts
		Mesh innerMesh = meshTriangleFan(innerRingPoints,
				params.backgroundColor);
		Mesh styleMesh = meshTrianglesRing(innerRingPoints, styleRingPoints,
				params.backgroundColor);
		Mesh outerMesh = meshTrianglesRing(styleRingPoints, outerRingPoints,
				params.outlineColor);
		if (params.type == SpeechBalloonType.THINK) {
			if (params.cloudCircleCount > 0 && params.cloudPointCount > 0) {
				Array<Vector2> circleInnerRingPoints = new Array<Vector2>();
				Array<Vector2> circleOuterRingPoints = new Array<Vector2>();
				Vector2 start = new Vector2(outerRingPoints.get(minTargetIndex));
				Vector2 direction = new Vector2(params.target).sub(start);
				float innerRadius = (direction.len() / params.cloudCircleCount) / 2;
				float outerRadius = innerRadius + params.outlineWidth;
				for (int i = 0; i < params.cloudCircleCount; i++) {
					dir.set(direction).nor()
							.scl((i * (innerRadius * 2)) - innerRadius);
					pos.set(start).add(dir);
					circleInnerRingPoints.clear();
					circleOuterRingPoints.clear();
					for (int j = 0; j < params.cloudPointCount; j++) {
						float degrees = 360 * ((float) j / params.cloudPointCount);
						dir.setAngle(degrees).nor().scl(innerRadius * 0.5f);
						circleInnerRingPoints.add(new Vector2(pos).add(dir));
						dir.setAngle(degrees).nor().scl(outerRadius * 0.5f);
						circleOuterRingPoints.add(new Vector2(pos).add(dir));
					}
					Mesh circleInnerMesh = meshTriangleFan(
							circleInnerRingPoints, params.backgroundColor);
					Mesh circleOuterMesh = meshTrianglesRing(
							circleInnerRingPoints, circleOuterRingPoints,
							params.outlineColor);
					modelBuilder.part("circleInner" + i, circleInnerMesh,
							GL20.GL_TRIANGLE_FAN, new Material());
					modelBuilder.part("circleOuter" + i, circleOuterMesh,
							GL20.GL_TRIANGLES, new Material());
				}
			}
		}
		modelBuilder.part("inner", innerMesh, GL20.GL_TRIANGLE_FAN,
				new Material());
		modelBuilder
				.part("style", styleMesh, GL20.GL_TRIANGLES, new Material());
		modelBuilder
				.part("outer", outerMesh, GL20.GL_TRIANGLES, new Material());

		// Return model
		return modelBuilder.end();
	}

	private static float averageDerivativeForPath(Path<Vector2> path,
			int samples) {
		float averageDerivative = 0f;
		for (float i = 0; i < 1f; i += 1f / samples) {
			path.derivativeAt(derivative, i);
			averageDerivative += derivative.len();
		}
		return averageDerivative / samples;
	}

	private static Mesh meshTriangleFan(Array<Vector2> ringPoints, Color color) {
		return meshTriangleFan(ringPoints, color, 0f);
	}

	private static Mesh meshTriangleFan(Array<Vector2> ringPoints, Color color,
			float z) {

		// Points
		int vertexCount = ringPoints.size;
		int indexCount = ringPoints.size;
		Mesh mesh = new Mesh(true, vertexCount, indexCount,
				new VertexAttribute(Usage.Position, POSITION, "a_position"),
				new VertexAttribute(Usage.ColorUnpacked, COLOR, "a_color"));

		// Set vertices
		float[] vertices = new float[vertexCount * (POSITION + COLOR)];
		int verticesIndex = 0;
		for (int i = 0; i < ringPoints.size; i++) {
			vertices[verticesIndex++] = ringPoints.get(i).x; // x
			vertices[verticesIndex++] = ringPoints.get(i).y; // y
			vertices[verticesIndex++] = z; // z
			vertices[verticesIndex++] = color.r; // r
			vertices[verticesIndex++] = color.g; // g
			vertices[verticesIndex++] = color.b; // b
			vertices[verticesIndex++] = color.a; // a
		}

		// Calculate indices
		short[] indices = new short[indexCount];
		int indicesIndex = 0;
		for (int i = 0; i < ringPoints.size; i++) {
			indices[indicesIndex++] = (short) i;
		}

		// Set arrays
		mesh.setVertices(vertices);
		mesh.setIndices(indices);
		return mesh;
	}

	private static Mesh meshTrianglesRing(Array<Vector2> innerRingPoints,
			Array<Vector2> outerRingPoints, Color color) {
		return meshTrianglesRing(innerRingPoints, outerRingPoints, color, 0f);
	}

	private static Mesh meshTrianglesRing(Array<Vector2> innerRingPoints,
			Array<Vector2> outerRingPoints, Color color, float z) {

		// Points
		int vertexCount = innerRingPoints.size * 2;
		int indexCount = vertexCount * 3;
		Mesh mesh = new Mesh(true, vertexCount, indexCount,
				new VertexAttribute(Usage.Position, POSITION, "a_position"),
				new VertexAttribute(Usage.ColorUnpacked, COLOR, "a_color"));

		// Set vertices
		float[] vertices = new float[vertexCount * (POSITION + COLOR)];
		int verticesIndex = 0;
		for (int i = 0; i < innerRingPoints.size; i++) {
			vertices[verticesIndex++] = innerRingPoints.get(i).x; // x
			vertices[verticesIndex++] = innerRingPoints.get(i).y; // y
			vertices[verticesIndex++] = z; // z
			vertices[verticesIndex++] = color.r; // r
			vertices[verticesIndex++] = color.g; // g
			vertices[verticesIndex++] = color.b; // b
			vertices[verticesIndex++] = color.a; // a
			vertices[verticesIndex++] = outerRingPoints.get(i).x; // x
			vertices[verticesIndex++] = outerRingPoints.get(i).y; // y
			vertices[verticesIndex++] = z; // z
			vertices[verticesIndex++] = color.r; // r
			vertices[verticesIndex++] = color.g; // g
			vertices[verticesIndex++] = color.b; // b
			vertices[verticesIndex++] = color.a; // a
		}

		// Calculate indices
		short[] indices = new short[indexCount];
		int indicesIndex = 0;
		for (int i = 0; i < innerRingPoints.size - 1; i++) {
			indices[indicesIndex++] = (short) ((i * 2) + 0);
			indices[indicesIndex++] = (short) ((i * 2) + 1);
			indices[indicesIndex++] = (short) ((i * 2) + 2);
			indices[indicesIndex++] = (short) ((i * 2) + 1);
			indices[indicesIndex++] = (short) ((i * 2) + 3);
			indices[indicesIndex++] = (short) ((i * 2) + 2);
		}
		indices[indicesIndex++] = (short) (vertexCount - 2);
		indices[indicesIndex++] = (short) (vertexCount - 1);
		indices[indicesIndex++] = (short) 0;
		indices[indicesIndex++] = (short) (vertexCount - 1);
		indices[indicesIndex++] = (short) 1;
		indices[indicesIndex++] = (short) 0;

		// Set arrays
		mesh.setVertices(vertices);
		mesh.setIndices(indices);
		return mesh;
	}

	private static Vector2 calculateNormal(Vector2 out, Vector2 position,
			Vector2 direction, Vector2 origin) {
		dir3D.set(direction.x, direction.y, 0);
		up3D.set(position.x - origin.x, position.y - origin.y, 0);
		crs3D.set(up3D).crs(dir3D);
		up3D.set(dir3D).crs(crs3D);
		out.set(up3D.x, up3D.y).nor();
		return out;
	}

	private static int validateIndex(int index, int size) {
		if (index >= size) {
			index = 0;
		} else if (index < 0) {
			index = size - 1;
		}
		return index;
	}

}
