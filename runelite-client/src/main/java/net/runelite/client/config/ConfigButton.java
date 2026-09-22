package net.runelite.client.config;

import java.util.Objects;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.runelite.client.config.ConfigSerializer;

/**
 * Marker type for rendering a clickable button in the config panel.
 */
@Getter
@NoArgsConstructor
@ConfigSerializer(ConfigButtonSerializer.class)
public class ConfigButton
{
	private String id = UUID.randomUUID().toString();

	public ConfigButton(String id)
	{
		this.id = id;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o)
		{
			return true;
		}
		if (!(o instanceof ConfigButton))
		{
			return false;
		}
		ConfigButton that = (ConfigButton) o;
		return Objects.equals(id, that.id);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(id);
	}
}
