package com.riferrei.streaming.pacman;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;

import com.amazon.ask.AlexaSkill;
import com.amazon.ask.Skill;
import com.amazon.ask.Skills;
import com.amazon.ask.exception.AskSdkException;
import com.amazon.ask.request.SkillRequest;
import com.amazon.ask.request.impl.BaseSkillRequest;
import com.amazon.ask.response.SkillResponse;
import com.amazon.ask.util.ValidationUtils;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.amazonaws.util.IOUtils;

import com.riferrei.streaming.pacman.alexa.AlexaDetailsHandler;
import com.riferrei.streaming.pacman.alexa.AlexaHelpHandler;
import com.riferrei.streaming.pacman.alexa.AlexaPlayersHandler;
import com.riferrei.streaming.pacman.alexa.AlexaStopHandler;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Tracer;

public class AlexaHandler implements RequestStreamHandler {

	private static final Tracer tracer =
		GlobalOpenTelemetry.getTracer("alexa-handler-tracer");

	@SuppressWarnings("rawtypes")
	private List<AlexaSkill> skills = Collections.singletonList(
		ValidationUtils.assertNotNull(getSkill(), "skill"));

	private Skill getSkill() {
		return Skills.standard()
			.addRequestHandlers(List.of(
				new AlexaHelpHandler(tracer),
				new AlexaPlayersHandler(tracer),
				new AlexaDetailsHandler(tracer),
				new AlexaStopHandler(tracer)
			))
			.build();
	}

	@Override @SuppressWarnings("rawtypes")
	public void handleRequest(InputStream input, OutputStream output,
		Context context) throws IOException {
		SkillRequest skillRequest = new BaseSkillRequest(IOUtils.toByteArray(input));
		for (AlexaSkill skill : skills) {
			SkillResponse skillResponse = skill.execute(skillRequest, context);
			if (skillResponse != null) {
				if (skillResponse.isPresent()) {
					skillResponse.writeTo(output);
				}
				return;
			}
		}
        throw new AskSdkException("Could not find a skill to handle the incoming request");
	}

}
