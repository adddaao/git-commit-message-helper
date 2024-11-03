package com.fulinlin.ui.setting.description;

import com.fulinlin.utils.VelocityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class DescriptionRead {
	private static final Logger log = LoggerFactory.getLogger(DescriptionRead.class);

	public static String readHtmlFile() {
		StringBuilder content = new StringBuilder();
		try (InputStream inputStream = DescriptionRead.class.getResourceAsStream("/includes/defaultDescription.html")) {
			if (inputStream != null) {
				InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
				BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
				String line;
				while ((line = bufferedReader.readLine()) != null) {
					content.append(line).append("\n");
				}
			}
		} catch (Exception e) {
			log.error("", e);
		}
		return VelocityUtils.convertDescription(content.toString());
	}


}
