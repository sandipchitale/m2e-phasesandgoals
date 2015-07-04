package maven.spy;

public interface IMavenBuildSpySink {
	enum STATUS {NONE, STARTED, OK, KO}
	public void clear(); 
	public void message(String message); 
	public void message(String message, STATUS status); 
	public void message(String message, STATUS status, Exception e); 
	public boolean isVisible();
	public void setVisible(boolean show);
	public void await();
}
