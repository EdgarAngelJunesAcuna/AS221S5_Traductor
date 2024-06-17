package pe.edu.vallegrande.TranslatorText.service;

import java.io.IOException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import pe.edu.vallegrande.TranslatorText.model.Translation;
import pe.edu.vallegrande.TranslatorText.repository.TranslationTextRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class TranslatorTextService {

	private static final String TRANSLATOR_KEY = "91b6a3785f444aa8be105167862b6daa";
	private static final String TRANSLATOR_LOCATION = "eastus";
	private static final String TRANSLATOR_ENDPOINT = "https://api.cognitive.microsofttranslator.com"; // Ingresar Key
	private static final String TRANSLATOR_ROUTE = "/translate?api-version=3.0";
	private static final String TRANSLATOR_URL = TRANSLATOR_ENDPOINT.concat(TRANSLATOR_ROUTE);

	private final OkHttpClient client;
	private final TranslationTextRepository translatorTextRepository;

	public TranslatorTextService(TranslationTextRepository translatorTextRepository) {
		this.client = new OkHttpClient();
		this.translatorTextRepository = translatorTextRepository;
	}

	public Mono<String> translateText(String text, String from, String to) {
		return Mono.fromCallable(() -> {
			MediaType mediaType = MediaType.parse("application/json");
			String requestBody = "[{\"Text\": \"" + text + "\"}]";
			String urlWithParams = TRANSLATOR_URL + "&from=" + from + "&to=" + to;
			RequestBody body = RequestBody.create(mediaType, requestBody);
			Request request = new Request.Builder().url(urlWithParams).post(body)
					.addHeader("Ocp-Apim-Subscription-Key", TRANSLATOR_KEY)
					.addHeader("Ocp-Apim-Subscription-Region", TRANSLATOR_LOCATION)
					.addHeader("Content-type", "application/json").build();
			try (Response response = client.newCall(request).execute()) {
				if (response.isSuccessful()) {
					String responseBody = response.body().string();
					String translatedText = extractTranslatedText(responseBody);
					return translatedText != null ? translatedText : "Translation not found";
				} else {
					throw new IOException("Unexpected response code: " + response);
				}
			}
		}).onErrorMap(error -> {
			log.error("Error translating text: {}", error.getMessage());
			return new IOException("Error translating text", error);
		});
	}

	private String extractTranslatedText(String responseBody) {
		JsonParser parser = new JsonParser();
		JsonArray translationsArray = parser.parse(responseBody).getAsJsonArray();
		if (translationsArray.size() > 0) {
			JsonArray translations = translationsArray.get(0).getAsJsonObject().getAsJsonArray("translations");
			if (translations.size() > 0) {
				JsonElement translation = translations.get(0);
				return translation.getAsJsonObject().get("text").getAsString();
			}
		}
		return null;
	}

	public Mono<String> editTranslation(Long translationId, String newText, String from, String to) {
		return translatorTextRepository.findById(translationId).flatMap(translation -> {
			// Aquí realizamos la nueva traducción
			return translateText(newText, from, to).flatMap(newTranslatedText -> {
				// Actualizamos el objeto de traducción con el nuevo texto
				translation.setTranslated_text(newTranslatedText);
				translation.setRequest_text(newText);
				translation.setFrom_lang(from);
				translation.setTo_lang(to);
				// Guardamos la traducción actualizada en la base de datos
				return translatorTextRepository.save(translation);
			});
		}).map(Translation::getTranslated_text).switchIfEmpty(
				Mono.error(new RuntimeException("Traducción no encontrada con el ID: " + translationId)));
	}

	public Flux<Translation> findAllActives() {
		return translatorTextRepository.findAllByStatus("A");
	}

	public Flux<Translation> findAllInactives() {
		return translatorTextRepository.findAllByStatus("I");
	}

	public Mono<Translation> findById(Long id) {
		return translatorTextRepository.findById(id);
	}

	@Transactional
	public Mono<Translation> save(Translation translation) {
		translation.setStatus("A"); // Set the default status to "A" for active
		return translatorTextRepository.save(translation);
	}

	@Transactional
	public Mono<Translation> activate(Long id) {
		return translatorTextRepository.findById(id).flatMap(translation -> {
			translation.setStatus("A");
			return translatorTextRepository.save(translation);
		});
	}

	@Transactional
	public Mono<Translation> deactivate(Long id) {
		// This method is already in place and should logically deactivate the translation
		return translatorTextRepository.findById(id)
				.flatMap(translation -> {
					translation.setStatus("I");
					return translatorTextRepository.save(translation);
				});
	}

	public Mono<Void> deleteTranslation(Long id) {
		return translatorTextRepository.deleteById(id);
	}
}
