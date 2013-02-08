# See usage function.
#
# Description:
# Script to create new remote branches in Git. 
#
# Options:
# See usage function.
#========================================================================================
#========================================================================================
# Description: Display usage information then abort the script.
#========================================================================================

#include the utility library
source `dirname $0`/rhq_bash.lib

usage() 
{
   USAGE=$(
cat << EOF
USAGE:   remote-branch.sh OPTIONS

   --source-branch=git_branch             [REQUIRED]
      The branch to base the new remote branch on. Ex. release/jon3.1.x, or master.  Script assumes the 
      most recent commits from the 'source-branch' are to be replicated to the new 'release-branch'. 

   --new-remote-branch=git_branch_name    [REQUIRED]
      Git branch name to be created. Assumes no such local or remote branch by this name already exists.

EOF
)

   EXAMPLE="remote-branch.sh --source-branch=\"release/jon3.1.x\" --new-remote-branch=\"rc/jon3.1.2.GA\""

   abort "$@" "$USAGE" "$EXAMPLE"
}

#========================================================================================
# Description: Validate and parse input arguments
#========================================================================================
parse_and_validate_options()
{
   print_function_information $FUNCNAME

   SOURCE_BRANCH=
   NEW_REMOTE_BRANCH=

   short_options="h"
   long_options="help,source-branch:,new-remote-branch:"

   PROGNAME=${0##*/}
   ARGS=$(getopt -s bash --options $short_options --longoptions $long_options --name $PROGNAME -- "$@" )
   eval set -- "$ARGS"

   while true; do
	   case $1 in
         -h|--help)
            usage
            ;;
         --source-branch)
            shift
            SOURCE_BRANCH="$1"
            shift
            ;;
         --new-remote-branch)
            shift
            NEW_REMOTE_BRANCH="$1"
            shift
            ;;
         --)
            shift
            break
            ;;
         *)
            usage
            ;;
	   esac
   done

   if [ -z "$SOURCE_BRANCH" ];
   then
      usage "Git source branch not specified!"
   fi

   if [ -z "$NEW_REMOTE_BRANCH" ];
   then
      usage "New remote branch name not specified!"
   fi

   print_centered "Script Options"
   script_options=( "SOURCE_BRANCH" "NEW_REMOTE_BRANCH")
   print_variables "${script_options[@]}"
}

#========================================================================================
# Description: Set all the local and environment variables required by the script.
#========================================================================================
synch_build_branch_and_push()
{

# checkout the source branch
echo "git checkout $GIT_SOURCE_BRANCH"
git checkout $GIT_SOURCE_BRANCH

# fetch all changes in remotes since last fetch
echo "git fetch --all"
git fetch --all

# synch the current branch with the latest on source branch
echo "git pull --rebase"
git pull --rebase

# echo current local branches
echo "git branch"
git branch

# create new git branch with last commit to source branch
echo "git checkout -b $NEW_REMOTE_BRANCH"
git checkout -b $NEW_REMOTE_BRANCH

# push new local branch remote. Local and remote branch has same name.
echo "git push origin $NEW_REMOTE_BRANCH:$NEW_REMOTE_BRANCH"
git push origin $NEW_REMOTE_BRANCH:$NEW_REMOTE_BRANCH

# set local branch to track remote git branch of the same name
echo "branch --set-upstream $NEW_REMOTE_BRANCH origin/$NEW_REMOTE_BRANCH"
git branch --set-upstream $NEW_REMOTE_BRANCH origin/$NEW_REMOTE_BRANCH

}

############ MAIN SCRIPT ############

parse_and_validate_options $@

synch_build_branch_and_push
