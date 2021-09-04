package de.invesdwin.maven.plugin;

import org.jsonschema2pojo.AbstractRuleLogger;

public class DisabledRuleLogger extends AbstractRuleLogger {

	public static final DisabledRuleLogger INSTANCE = new DisabledRuleLogger();

	private DisabledRuleLogger() {
	}

	@Override
	public boolean isDebugEnabled() {
		return false;
	}

	@Override
	public boolean isErrorEnabled() {
		return false;
	}

	@Override
	public boolean isInfoEnabled() {
		return false;
	}

	@Override
	public boolean isTraceEnabled() {
		return false;
	}

	@Override
	public boolean isWarnEnabled() {
		return false;
	}

	@Override
	protected void doDebug(final String msg) {
	}

	@Override
	protected void doError(final String msg, final Throwable e) {
	}

	@Override
	protected void doInfo(final String msg) {
	}

	@Override
	protected void doTrace(final String msg) {
	}

	@Override
	protected void doWarn(final String msg, final Throwable e) {
	}

}
