

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.io.IOUtils;


/* Example of a commit
 * Commit SHA: 7d6a91f18f627042e5f436030e36ebe7bb747244
 * Author: Parvez <mua237@mail.usask.ca>
 * Date: Mon Apr 16 02:05:33 2018 -0400
 * 
 * Message: update diff between two revisions and collect the changed lines
 * ...
 * */

public class CommitManager {

	private ArrayList<Commit> commitList; //starts from index zero which is the most recent commit
	private String repositoryPath;
	private HashMap<String,Integer> hmSHAtoCommitIndex;
	
	public CommitManager(String _repositoryPath) {
		this.commitList = new ArrayList();
		this.repositoryPath = _repositoryPath;
		this.hmSHAtoCommitIndex = new HashMap();
	}
	public void add(Commit commit) {
		commitList.add(commit);
		hmSHAtoCommitIndex.put(commit.getSha(),commitList.size()-1);
	}
	public boolean hasCommitBySHA(String SHA) {
		if(hmSHAtoCommitIndex.containsKey(SHA))
			return true;
		else return false;
	}
	
	//the goal of this function is to collect all commits and arrange them based on the time
	public void run() throws InterruptedException, IOException {
		ProcessBuilder pb = new ProcessBuilder("git","log");
		pb.directory(new File(this.getRepositoryPath()));
		
		Process process = pb.start();
		int errCode = process.waitFor();
		String output = ProcessUtility.output(process.getInputStream());

		this.parse(output);
	}
	
	//commit messages are typically followed by space. We remove those spaces
	private String listToString(List<String> lineList) {
		StringBuilder sb = new StringBuilder("");
		for(String line:lineList) {
			if(line.matches("\\s+")==false)
			sb.append(line.trim());
		}
		return sb.toString();
	}
	
	private void parse(String output) throws IOException {
		String author=null;
		String commitSHA=null;
		String date  =null;
		String message=null;
		ArrayList<String> messageLines = null;
		
		//step-1:convert the output into list of lines 
		List<String> lineList = IOUtils.readLines(new StringReader(output));
		
		//step-2: read the commits
		for(int i=0;i<lineList.size();i++) {
			String line = lineList.get(i);
			if(line.startsWith("commit ")) {
				String split[]=line.split("\\s+");
				commitSHA = split[1];i++;
				
				if(lineList.get(i).startsWith("Author:") && lineList.get(i+1).startsWith("Date:")) {
					author= lineList.get(i).substring("Author: ".length()).trim(); i++;
					date  = lineList.get(i).substring("Date: ".length()).trim(); i++;
					
					//now read the messages
					messageLines = new ArrayList();
					for(;i<lineList.size();i++) {
						line = lineList.get(i);
						if(line.startsWith("commit ")) { //this indicates the start of another commit
							i--;
							break;
						}else {
							messageLines.add(line);
						}
					}
					Commit commit = new Commit(author,date,this.listToString(messageLines),commitSHA);
					this.add(commit);
					commit.print();
					
				}else {
					throw new RuntimeException("Error in parsing output");
				}
			}
		}
	}

	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		CommitManager commitManager = new CommitManager("/home/parvez/research/repos/CodeCompletionEvaluation");
		try {
			commitManager.run();
		} catch (InterruptedException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public ArrayList<Commit> getCommitList() {
		return commitList;
	}
	public String getRepositoryPath() {
		return repositoryPath;
	}

}
