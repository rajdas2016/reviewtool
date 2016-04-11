package de.setsoftware.reviewtool.slicesources.svn;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import org.tmatesoft.svn.core.ISVNLogEntryHandler;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNLogEntryPath;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNRevision;

import de.setsoftware.reviewtool.base.Pair;
import de.setsoftware.reviewtool.base.ReviewtoolException;
import de.setsoftware.reviewtool.diffalgorithms.IDiffAlgorithm;
import de.setsoftware.reviewtool.diffalgorithms.SimpleTextDiffAlgorithm;
import de.setsoftware.reviewtool.model.changestructure.FileInRevision;
import de.setsoftware.reviewtool.model.changestructure.Fragment;
import de.setsoftware.reviewtool.model.changestructure.ISliceSource;
import de.setsoftware.reviewtool.model.changestructure.PositionInText;
import de.setsoftware.reviewtool.model.changestructure.RepoRevision;
import de.setsoftware.reviewtool.model.changestructure.Revision;
import de.setsoftware.reviewtool.model.changestructure.Slice;

/**
 * A simple slice source that makes every commit a slice and every continuous change segment a fragment.
 */
public class SvnSliceSource implements ISliceSource {

    private static final String KEY_PLACEHOLDER = "${key}";
    private static final int LOOKUP_LIMIT = 1000;

    private final Set<File> workingCopyRoots;
    private final String logMessagePattern;
    private final SVNClientManager mgr = SVNClientManager.newInstance();
    private final IDiffAlgorithm diffAlgorithm = new SimpleTextDiffAlgorithm();


    public SvnSliceSource(List<File> projectRoots, String logMessagePattern) {
        this.workingCopyRoots = this.determineWorkingCopyRoots(projectRoots);

        this.logMessagePattern = logMessagePattern;
        //test that the pattern can be parsed
        this.createPatternForKey("TEST-123");
    }

    private Set<File> determineWorkingCopyRoots(List<File> projectRoots) {
        final LinkedHashSet<File> workingCopyRoots = new LinkedHashSet<>();
        for (final File projectRoot : projectRoots) {
            final File wcRoot = this.determineWorkingCopyRoot(projectRoot);
            if (wcRoot != null) {
                workingCopyRoots.add(wcRoot);
            }
        }
        return workingCopyRoots;
    }

    private File determineWorkingCopyRoot(File projectRoot) {
        File curPotentialRoot = projectRoot;
        while (!this.isPotentialRoot(curPotentialRoot)) {
            curPotentialRoot = curPotentialRoot.getParentFile();
            if (curPotentialRoot == null) {
                return null;
            }
        }
        while (true) {
            final File next = curPotentialRoot.getParentFile();
            if (next == null || !this.isPotentialRoot(next)) {
                return curPotentialRoot;
            }
            curPotentialRoot = next;
        }
    }

    private boolean isPotentialRoot(File next) {
        final File dotsvn = new File(next, ".svn");
        return dotsvn.isDirectory();
    }

    private Pattern createPatternForKey(String key) {
        return Pattern.compile(this.logMessagePattern.replace(KEY_PLACEHOLDER, Pattern.quote(key)));
    }

    /**
     * Handler that filters log entries with the given pattern.
     */
    private class LookupHandler implements ISVNLogEntryHandler {

        private final Pattern pattern;
        private final List<Pair<SVNURL, SVNLogEntry>> matchingEntries = new ArrayList<>();
        private SVNURL currentRoot;

        public LookupHandler(Pattern patternForKey) {
            this.pattern = patternForKey;
        }

        public void setCurrentRoot(SVNURL workingCopyRoot) {
            this.currentRoot = workingCopyRoot;
        }

        @Override
        public void handleLogEntry(SVNLogEntry logEntry) throws SVNException {
            if (logEntry.getMessage() != null && this.pattern.matcher(logEntry.getMessage()).matches()) {
                assert this.currentRoot != null;
                this.matchingEntries.add(Pair.create(this.currentRoot, logEntry));
            }
        }

    }

    @Override
    public List<Slice> getSlices(String key) {
        try {
            final List<Pair<SVNURL, SVNLogEntry>> revisions = this.determineRelevantRevisions(key);
            this.sortByDate(revisions);
            return this.convertToSlices(revisions);
        } catch (final SVNException | IOException e) {
            throw new ReviewtoolException(e);
        }
    }

    private List<Pair<SVNURL, SVNLogEntry>> determineRelevantRevisions(String key) throws SVNException {
        final LookupHandler handler = new LookupHandler(this.createPatternForKey(key));
        for (final File workingCopyRoot : this.workingCopyRoots) {
            final SVNURL rootUrl = this.mgr.getLogClient().getReposRoot(workingCopyRoot, null, SVNRevision.HEAD);
            handler.setCurrentRoot(rootUrl);
            this.mgr.getLogClient().doLog(
                    rootUrl,
                    new String[] {"/"},
                    SVNRevision.HEAD,
                    SVNRevision.create(0),
                    SVNRevision.HEAD,
                    false,
                    true,
                    false,
                    LOOKUP_LIMIT,
                    new String[0],
                    handler);
        }
        return handler.matchingEntries;
    }

    private void sortByDate(List<Pair<SVNURL, SVNLogEntry>> revisions) {
        Collections.sort(revisions, new Comparator<Pair<SVNURL, SVNLogEntry>>() {
            @Override
            public int compare(Pair<SVNURL, SVNLogEntry> o1, Pair<SVNURL, SVNLogEntry> o2) {
                return o1.getSecond().getDate().compareTo(o2.getSecond().getDate());
            }
        });
    }

    private List<Slice> convertToSlices(List<Pair<SVNURL, SVNLogEntry>> revisions)
            throws SVNException, IOException {
        final List<Slice> ret = new ArrayList<>();
        for (final Pair<SVNURL, SVNLogEntry> e : revisions) {
            ret.add(this.convertToSlice(e));
        }
        return ret;
    }

    private Slice convertToSlice(Pair<SVNURL, SVNLogEntry> e) throws SVNException, IOException {
        return new Slice(e.getSecond().getMessage() + " (Rev. " + e.getSecond().getRevision() + ")",
                this.determineFragments(e));
    }

    private List<Fragment> determineFragments(Pair<SVNURL, SVNLogEntry> e)
            throws SVNException, IOException {
        final List<Fragment> ret = new ArrayList<>();
        final SVNRevision revision = SVNRevision.create(e.getSecond().getRevision());
        final Set<String> moveSources = this.determineMoveSources(e.getSecond().getChangedPaths().values());
        for (final Entry<String, SVNLogEntryPath> entry : e.getSecond().getChangedPaths().entrySet()) {
            if (entry.getValue().getKind() != SVNNodeKind.FILE) {
                continue;
            }
            if (moveSources.contains(entry.getValue().getPath())) {
                //Moves are contained twice, as a copy and a deletion. The deletion shall not result in a fragment.
                continue;
            }
            ret.addAll(this.determineFragments(
                    revision, e.getFirst(), entry.getValue()));
        }
        return ret;
    }

    private List<Fragment> determineFragments(SVNRevision revision, SVNURL repoUrl, SVNLogEntryPath entryInfo)
            throws SVNException, IOException {
        final String oldPath = entryInfo.getCopyPath() == null ? entryInfo.getPath() : entryInfo.getCopyPath();
        final byte[] oldFile = this.loadFile(repoUrl, oldPath, revision.getNumber() - 1);
        final byte[] newFile = this.loadFile(repoUrl, entryInfo.getPath(), revision.getNumber());

        final List<Fragment> ret = new ArrayList<>();
        for (final Pair<PositionInText, PositionInText> pos : this.diffAlgorithm.determineDiff(
                oldFile, newFile, "ISO-8859-1")) {
            ret.add(new Fragment(
                    new FileInRevision(entryInfo.getPath(), this.mapRevision(revision)),
                    pos.getFirst(),
                    pos.getSecond()));
        }
        return ret;
    }

    private Set<String> determineMoveSources(Collection<SVNLogEntryPath> entries) {
        final Set<String> ret = new LinkedHashSet<>();

        //determine all copy sources
        for (final SVNLogEntryPath p : entries) {
            if (p.getCopyPath() != null) {
                ret.add(p.getCopyPath());
            }
        }

        //if a copy source was deleted, we consider this a "move", everything else is not a move
        for (final SVNLogEntryPath p : entries) {
            if (p.getType() != 'D') {
                ret.remove(p.getPath());
            }
        }

        return ret;
    }

    private byte[] loadFile(SVNURL repoUrl, String path, long revision) throws SVNException {
        final SVNRepository repo = this.mgr.getRepositoryPool().createRepository(repoUrl, true);
        final ByteArrayOutputStream contents = new ByteArrayOutputStream();
        if (repo.checkPath(path, revision) != SVNNodeKind.FILE) {
            return new byte[0];
        }
        repo.getFile(path, revision, null, contents);
        return contents.toByteArray();
    }

    private Revision mapRevision(SVNRevision revision) {
        return new RepoRevision(revision.getNumber());
    }

    //TEST
    public static void main(String[] args) {
        final File f = new File("C:\\testworkspace\\testprojekt");
        final SvnSliceSource src = new SvnSliceSource(Collections.singletonList(f), ".*${key}([^0-9].*)?");
        System.out.println(src.getSlices("PSY-12"));
        System.out.println(src.getSlices("tralala"));
        System.out.println(src.getSlices("PSY-123"));
    }

}