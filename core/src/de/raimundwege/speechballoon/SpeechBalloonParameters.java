package de.raimundwege.speechballoon;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;

import de.raimundwege.speechballoon.SpeechBalloonUtils.SpeechBalloonType;

public class SpeechBalloonParameters {

	private final static Vector2 dir = new Vector2();

	private final Vector2 origin = new Vector2();
	private final Vector2 halfSize = new Vector2();

	public final Vector2 position = new Vector2();
	public final Vector2 size = new Vector2();
	public final Vector2 target = new Vector2();

	public final Color backgroundColor = new Color();
	public final Color outlineColor = new Color();

	public float targetScale;

	public int cloudCircleCount;
	public int cloudCount;
	public int cloudHeight;
	public int cloudPointCount;
	public int outlineWidth;
	public int pointCount;

	public SpeechBalloonType type;

	public SpeechBalloonParameters() {
		backgroundColor.set(Color.WHITE);
		outlineColor.set(Color.BLACK);
		outlineWidth = 2;
		pointCount = 90;
		cloudCount = 6;
		cloudHeight = 5;
		cloudCircleCount = 6;
		cloudPointCount = 24;
		targetScale = 0.7f;
		type = SpeechBalloonType.NORMAL;
	}

	public Vector2 origin() {
		return origin.set(position).add(halfSize());
	}

	public Vector2 halfSize() {
		return halfSize.set(size).scl(0.5f);
	}

	public float targetAngle() {
		return dir.set(target).sub(origin()).angle();
	}

}
