package top.quantic.sentry.discord;

import org.springframework.stereotype.Component;
import top.quantic.sentry.discord.core.Command;
import top.quantic.sentry.discord.module.CommandSupplier;

import java.util.List;

@Component
public class Tickets implements CommandSupplier {

	@Override
	public List<Command> getCommands() {
		return null;
	}
}
