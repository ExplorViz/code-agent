# code-agent

This project is a tool that analyzes a git repository containg a java project.

## Usage and Run Modes

<!-- TODO Describe the usage, building etc. here -->
<!-- TODO run modes are - local, remote, CI -->

## Settings

As the code-agent is made to run against your specific repository, you have to change some settings before it is able to
analyze it. All settings can be manipulated via the ``application.properties`` file.

### Selecting the Repository Source

The repository source get selected automatically based on the settings. Decisive are the values of
the [local storage path](#explorvizgitanalysislocalstorage-path),
the [remote storage path](#explorvizgitanalysisremotestorage-path), the [remote url](#explorvizgitanalysisremoteurl) and
the [branch](#explorvizgitanalysisbranch). Source is selected as follows:

| local.storage-path | remote.storage-path | remote.url   | branch       | repository source                                          |
|--------------------|---------------------|--------------|--------------|------------------------------------------------------------|
| set                | set or empty        | set or empty | empty        | locally available repository, use current branch           |
| set                | set or empty        | set or empty | set          | locally available repository, checkout given branch        |
| empty              | set                 | set          | empty        | cloning repository to path, checkout default branch        |
| empty              | set                 | set          | set          | cloning repository to the path, checkout given branch      |
| empty              | empty               | set          | empty        | cloning repository to temp folder, checkout default branch |
| empty              | empty               | set          | empty        | cloning repository to temp folder, checkout given branch   |
| empty              | set or empty        | empty        | set or empty | INVALID                                                    |

To see the restrictions of the different settings values, consider its respective description.

### explorviz.gitanalysis.local.storage-path

Type: String or empty

This is the path to your local repository. It has to be an absolute path, any relative path may or may not work.

### explorviz.gitanalysis.remote.storage-path

Type: String or empty

This is the path to the storage folder for the repository. It can either be an absolute path, a relative path (
considering the execution folder as root) or omitted. If it is omitted, a folder called "TemporaryRepository" gets
created in the temp folder of your machine. Any successive execution will create a new temp folder and will clone the
repository again.

### explorviz.gitanalysis.remote.url

Type: String or empty

This is the remote url to the git repository. Only HTTP and HTTPS cloning is supported. If an SSH url is provided, it
gets converted to an HTTP url, if possible.

### explorviz.gitanalysis.branch

Type: String or empty

This is the branch name to be analyzed. If omitted the current ord default branch is used. The analysis always considers
all commits within the branch, starting with the first commit in the repository. If you only want to analyze the commits
since the creation of the branch, use the [start commit sha](#explorvizgitanalysisstart-commit-sha1) setting to start
the analysis with the first commit of the branch.

### explorviz.gitanalysis.remote.username

Type: String or empty

The username used to access and clone private repositories. Leave blank if you clone from a public repository.

### explorviz.gitanalysis.remote.password

Type: String or empty

The password used to access and clone private repositories. Leave blank if you clone from a public repository.

### explorviz.gitanalysis.fetch-remote-data

Type: Boolean or Empty (defaults to false)

If a remote storage is used and this is set to true, the analysis acts "state aware", fetching the latest state of the
analysis data for the given branch from the remote. The analysis begins with the first new commit and proceeds to the
latest available. [Start commit](#explorvizgitanalysisstart-commit-sha1)
and [end commit](#explorvizgitanalysisend-commit-sha1) settings are ignored.

If set to false, the analysis ignores any remote state but acts according to the restrictions given by
the [start commit](#explorvizgitanalysisstart-commit-sha1) and [end commit](#explorvizgitanalysisend-commit-sha1)
settings.

### explorviz.gitanalysis.send-to-remote

Type: Boolean or Empty (defaults to false)

If a remote storage is used and this is set to true, the analysis data will be sent to the remote endpoint, if set to
false, the analysis data will be stored as json on disc. The storage location will be printed
on startup and is relative to the java working directory.

### explorviz.gitanalysis.source-directory

Type: String or empty

To detect java types successfully, the source directory should be specified. Source files are expected to be somewhere
inside the given folders. Provide one or multiple [search expressions](#search-expressions).

### explorviz.gitanalysis.restrict-analysis-to-folders

Type: String or empty

Only java files contained in the reachable folders from this search expression are analyzed. Type detection is not
affected by this setting, any files reachable by the [source directory setting](#explorvizgitanalysissource-directory)
are used to detect the correct type. Provide one or multiple [search expressions](#search-expressions).

### explorviz.gitanalysis.start-commit-sha1

Type: String or empty

The full SHA-1 hash of a commit used to define the starting point of the analysis. The Commit must be reachable in the
given [branch](#explorvizgitanalysisbranch). The analysis includes the given commit.
If [fetching from the remote](#explorvizgitanalysisfetch-remote-data) is enabled, this setting is ignored.

### explorviz.gitanalysis.end-commit-sha1

Type: String or empty

The full SHA-1 hash of a commit used to define the end point of the analysis. The Commit must be reachable in the
given [branch](#explorvizgitanalysisbranch). This commit is included in the analysis, the analysis ends with the
analysis of this commit. If [fetching from the remote](#explorvizgitanalysisfetch-remote-data) is enabled, this setting
is ignored.

### explorviz.gitanalysis.calculate-metrics

Type: Boolean or Empty (defaults to false)

Enables the calculation of metrics that are added to the analysis data.

### explorviz.gitanalysis.assume-unresolved-types-from-wildcard-imports

Type: Boolean or Empty (defaults to false)

If wildcard imports are used in the java files, it is not possible to determine some types. If only a single wildcard
import in a file is detected, setting this setting to true results in "assuming the unresolvable types are defined in
the wildcard import". If disabled, no fully qualified name will be provided for these types.

If more than one wildcard import is found, this setting automatically is disabled for the file in question.

### Search Expressions

Search expressions are simple strings to define paths relative to the repository path. Multiple expressions can be
defined by simply comma-seperating them.

#### Wildcard

If the folder location changes over the course of the development, it is possible to use the wildcard character \* (
asterisk).

Leading Wildcard:

E.g. the folder location ``*/src/main/java`` is searched in the repository everywhere, until some folder hierarchy
matches ``src/main/java`` for example the path ``project/javasources/src/main/java``. The first match is used, if there
are multiple matches, try to specify the path even more. Keep in mind to not use a line separator in front of or after
the wildcard.

Infix Wildcards:

E.g. the folder location ``/src/*/java`` is searched in the repository starting with ``src`` and must end with ``java``.
The path ``src/main/java`` would match as well as ``/src/some/deep/hierarchy/to/serach/java``. If both folder structures
would exist, one of these could be returned as the folders are not searched in a specific order. Make sure the folder is
unambigous.

Single wildcards do not guarantee a deeper folder level, the search string ``/some/*/path`` will match the
folder ``some/path`` even if ``some/other/path/`` exists and might be the wanted.

Multiple consecutive wildcards e.g. ``/some/*/*/path`` enforce a depth of *number of wildcards - 1*. This search string
matches ``/some/other/path/``, ``/some/deeper/other/path/`` or even deeper but not ``/some/path/``.

**WARNING: paths ending with wildcards are not allowed!**

#### Optional Paths

If a folder, like the test folder, is not present from the first commit onwards, it is possible to define these type of
folders as optional. Optional folders are considered if they are found and ignored if not present.

To define a search expression as optional, simply put it in brackets: ``[\src\test\java]``. Wildcards are supported in
optional search expressions.