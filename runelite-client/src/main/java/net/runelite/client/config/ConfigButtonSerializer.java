package net.runelite.client.config;

public class ConfigButtonSerializer implements Serializer<ConfigButton>
{
	@Override
	public String serialize(ConfigButton configButton)
	{
		return configButton != null ? configButton.getId() : null;
	}

	@Override
	public ConfigButton deserialize(String string)
	{
		return string == null ? new ConfigButton() : new ConfigButton(string);
	}
}
