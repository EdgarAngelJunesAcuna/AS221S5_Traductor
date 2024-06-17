package pe.edu.vallegrande.TranslatorText.rest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.vallegrande.TranslatorText.model.TranslateRequestBody;
import pe.edu.vallegrande.TranslatorText.repository.TranslationTextRepository;
import pe.edu.vallegrande.TranslatorText.service.TranslatorTextService;
import pe.edu.vallegrande.TranslatorText.model.Translation;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import java.util.Map;

@RestController
@Slf4j
@RequestMapping("/translate")
public class TranslatorTextRest {

	private final TranslatorTextService translatorTextService;
	private final TranslationTextRepository translatorTextRepository;

	@Autowired
	public TranslatorTextRest(TranslatorTextService translatorTextService,
							  TranslationTextRepository translatorTextRepository) {
		this.translatorTextService = translatorTextService;
		this.translatorTextRepository = translatorTextRepository;
	}

	@PostMapping
	public Mono<ResponseEntity<String>> translateText(@RequestBody TranslateRequestBody requestBody) {
		String text = requestBody.getText();
		String from = requestBody.getFrom();
		String to = requestBody.getTo();
		return translatorTextService.translateText(text, from, to).flatMap(translatedText -> {
			Translation translation = new Translation();
			translation.setRequest_text(text);
			translation.setTranslated_text(translatedText);
			translation.setFrom_lang(from);
			translation.setTo_lang(to);
			return translatorTextRepository.save(translation).map(
					savedTranslation -> ResponseEntity.status(HttpStatus.OK).body("Translation saved successfully"));
		}).onErrorResume(error -> {
			log.error("Error translating text: {}", error.getMessage());
			return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error translating text"));
		});
	}

	@GetMapping("/all")
	public Flux<Translation> getAllTranslations() {
		return translatorTextRepository.findAll();
	}

	@PutMapping("/edit/{id}")
	public Mono<ResponseEntity<String>> editTranslation(@PathVariable Long id,
														@RequestBody Map<String, String> requestBody) {

		String newText = requestBody.get("request_text");
		String from = requestBody.get("from_lang");
		String to = requestBody.get("to_lang");

		return translatorTextService.editTranslation(id, newText, from, to)
				.map(updatedText -> ResponseEntity.ok().body(updatedText))
				.defaultIfEmpty(ResponseEntity.notFound().build());
	}

	@DeleteMapping("/delete/{id}")
	public Mono<ResponseEntity<Void>> deleteTranslation(@PathVariable Long id) {
		return translatorTextService.deactivate(id)
				.map(unused -> ResponseEntity.ok().<Void>build())
				.defaultIfEmpty(ResponseEntity.notFound().build());
	}

	@GetMapping("/actives")
	public Flux<Translation> getAllActives() {
		return translatorTextService.findAllActives();
	}

	@GetMapping("/inactives")
	public Flux<Translation> getAllInactives() {
		return translatorTextService.findAllInactives();
	}

	@GetMapping("/{id}")
	public Mono<ResponseEntity<Translation>> getTranslationById(@PathVariable Long id) {
		return translatorTextService.findById(id).map(translation -> ResponseEntity.ok().body(translation))
				.defaultIfEmpty(ResponseEntity.notFound().build());
	}

	@PutMapping("/activate/{id}")
	public Mono<ResponseEntity<String>> activateTranslation(@PathVariable Long id) {
		return translatorTextService.activate(id)
				.map(activatedTranslation -> ResponseEntity.ok().body("Translation activated successfully"))
				.defaultIfEmpty(ResponseEntity.notFound().build());
	}
}
