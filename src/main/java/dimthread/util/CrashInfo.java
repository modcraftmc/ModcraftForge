package dimthread.util;


import net.minecraft.crash.CrashReport;
import net.minecraft.crash.ReportedException;
import net.minecraft.world.server.ServerWorld;

public class CrashInfo {

	private final ServerWorld world;
	private final Throwable throwable;

	public CrashInfo(ServerWorld world, Throwable throwable) {
		this.world = world;
		this.throwable = throwable;
	}

	public ServerWorld getWorld() {
		return this.world;
	}

	public Throwable getThrowable() {
		return this.throwable;
	}

	public void crash(String title) {
		CrashReport report = CrashReport.makeCrashReport(this.getThrowable(), title);
		this.getWorld().fillCrashReport(report);
		throw new ReportedException(report);
	}

}
