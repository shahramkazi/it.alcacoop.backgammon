/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package it.alcacoop.backgammon.actions;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction;

public class MyAlphaAction extends TemporalAction {
	private float start, end;
	private Color color;

	protected void begin () {
		if (color == null) color = actor.getColor();
		start = color.a;
	}

	@Override
	protected void end() {
	  Gdx.graphics.setContinuousRendering(false);
    Gdx.graphics.requestRendering();
	}
	
	protected void update (float percent) {
		color.a = start + (end - start) * percent;
	}

	public void reset () {
		super.reset();
		color = null;
	}

	public Color getColor () {
		return color;
	}

	/** Sets the color to modify. If null (the default), the {@link #getActor() actor's} {@link Actor#getColor() color} will be
	 * used. */
	public void setColor (Color color) {
		this.color = color;
	}

	public float getAlpha () {
		return end;
	}

	public void setAlpha (float alpha) {
		this.end = alpha;
	}
}
