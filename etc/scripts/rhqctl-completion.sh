#!/bin/bash

# completion function for rhqctl command
# to auto load this script move this script to /etc/bash_completion.d/

_rhqctl() {
    local opts agentServerStorage storageInstallSubopts agentInstallSubopts serverInstallSubopts upgradeSubopts dataMingratorSubopts
    COMPREPLY=()

    cur="${COMP_WORDS[COMP_CWORD]}"
    prev="${COMP_WORDS[COMP_CWORD-1]}"
    opts="console install start status stop upgrade"
    agentServerStorage="--agent --server --storage"
    serverStorage="--server --storage"
    dataMigratorSubopts="none estimate print-command do-it"

    # the spaces in the beginning and in the end are important here
    storageInstallSubopts=" --storage-config --storage-data-root-dir "
    agentInstallSubopts=" --agent-auto-start --agent-config --agent-preference "
    serverInstallSubopts=" --server-config "
    upgradeSubopts=" --from-agent-dir --from-server-dir --agent-auto-start --run-data-migrator --storage-config --storage-data-root-dir --use-remote-storage-node "
    booleanSubopts=" --agent-auto-start --use-remote-storage-node "
    trueFalse="true false"

    if [[ "${COMP_LINE}" =~ ^\.?rhqctl[[:space:]]*$ ]] ; then
      COMPREPLY=( $(compgen -W "${opts}" -- ${cur}) )
      return 0
    fi

    first="${COMP_WORDS[1]}"

    if [[ "x${first}" == "xstart" ]] || [[ "x${first}" == "xstop" ]] || [[ "x${first}" == "xstatus" ]] ; then
        COMPREPLY=( $(compgen -W "${agentServerStorage}" -- ${cur}) )
        return 0
    elif  [[ "x${first}" == "xconsole" ]] ; then
        COMPREPLY=( $(compgen -W "${serverStorage}" -- ${cur}) )
        return 0
    elif [[ "x${first}" == "xupgrade" ]] ; then
        if [[ "${upgradeSubopts}" == *" ${prev} "* ]] ; then
          if [[ "x${prev}" == "x--run-data-migrator" ]] ; then
            COMPREPLY=( $(compgen -W "${dataMigratorSubopts}" -- ${cur}) )
            return 0
          else
            checkForBooleanSubopt ${prev} && return 0
            completePath ${cur}
            return 0
          fi
        fi
        COMPREPLY=( $(compgen -W "${upgradeSubopts}" -- ${cur}) )
        return 0
    elif [[ "x${first}" == "xinstall" ]] ; then
        second="${COMP_WORDS[2]}"
        if [[ "x$second" == "x" ]] ; then
            COMPREPLY=( $(compgen -W "${agentServerStorage}" -- ${cur}) )
            return 0
        fi

        if [[ "x${second}" == "x--server" ]] ; then
            if [[ "${serverInstallSubopts}" == *" ${prev} "* ]] ; then
                completePath ${cur}
                return 0
            fi
            COMPREPLY=( $(compgen -W "${serverInstallSubopts}" -- ${cur}) )
            return 0
        elif [[ "x${second}" == "x--agent" ]] ; then
            if [[ "${agentInstallSubopts}" == *" ${prev} "* ]] ; then
                checkForBooleanSubopt ${prev}  && return 0
                completePath ${cur}
                return 0
            fi
            COMPREPLY=( $(compgen -W "${agentInstallSubopts}" -- ${cur}) )
            return 0
        elif [[ "x${second}" == "x--storage" ]] ; then
            if [[ "${storageInstallSubopts}" == *" ${prev} "* ]] ; then
                completePath ${cur}
                return 0
            fi
            COMPREPLY=( $(compgen -W "${storageInstallSubopts}" -- ${cur}) )
            return 0
        fi
        COMPREPLY=( $(compgen -W "${agentServerStorage}" -- ${cur}) )
        return 0
    # fallback
    elif [[ "${cur}" == * ]] ; then
        COMPREPLY=( $(compgen -W "${opts}" -- ${cur}) )
        return 0
    fi
}

function completePath(){
  if [[ "${cur}x" == *"/x" ]] ; then
    toComplete="${cur}"
  else
    toComplete="${cur}*"
  fi
  COMPREPLY=( $(compgen -W "$( ls -1 ${toComplete} 2> /dev/null )" ) )
}

function checkForBooleanSubopt(){
  if [[ "${booleanSubopts}" == *" ${prev} "* ]] ; then
    COMPREPLY=( $(compgen -W "${trueFalse}" -- ${cur} ) )
    return 0
  else
    return 1
  fi
}

complete -F _rhqctl rhqctl
