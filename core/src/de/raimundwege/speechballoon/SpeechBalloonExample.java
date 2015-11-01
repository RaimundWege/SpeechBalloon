package de.raimundwege.speechballoon;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.utils.Array;

import de.raimundwege.speechballoon.SpeechBalloonUtils.SpeechBalloonType;

public class SpeechBalloonExample extends ApplicationAdapter {

	OrthographicCamera camera;
	Array<ModelInstance> instances;
	ModelBatch modelBatch;

	@Override
	public void create() {

		// Camera
		camera = new OrthographicCamera(Gdx.graphics.getWidth(),
				Gdx.graphics.getHeight());
		camera.position.x += camera.viewportWidth / 2;
		camera.position.y += camera.viewportHeight / 2;
		camera.update();

		// Model batch
		modelBatch = new ModelBatch();
		instances = new Array<ModelInstance>();

		SpeechBalloonParameters params = new SpeechBalloonParameters();
		params.target.set(200f, 50f);
		params.size.set(100f, 100f);
		params.position.set(50f, 50f);
		instances.add(SpeechBalloonUtils.createSpeechBalloon(params));

		params.position.set(350f, 50f);
		params.type = SpeechBalloonType.NORMAL;
		instances.add(SpeechBalloonUtils.createSpeechBalloon(params));

		params.backgroundColor.set(Color.BLACK);
		params.position.set(50f, 250f);
		params.type = SpeechBalloonType.SCREAM;
		params.outlineWidth = 5;
		params.outlineColor.set(Color.WHITE);
		params.pointCount = 100;
		instances.add(SpeechBalloonUtils.createSpeechBalloon(params));

		params.position.set(350f, 250f);
		params.type = SpeechBalloonType.THINK;
		params.cloudCount = 10;
		params.outlineWidth = 3;
		params.size.set(100, 100);
		instances.add(SpeechBalloonUtils.createSpeechBalloon(params));
	}

	@Override
	public void render() {
		Gdx.gl.glClearColor(1, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		modelBatch.begin(camera);
		for (ModelInstance instance : instances) {
			modelBatch.render(instance);
		}
		modelBatch.end();
	}

}
