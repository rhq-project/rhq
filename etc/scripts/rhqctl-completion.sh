#!/bin/bash

# completion function for rhqctl command
# to auto load this script move this script to /etc/bash_completion.d/

_rhqctl() {
    local cur prev opts agentServerStorage storageInstallSubopts agentInstallSubopts serverInstallSubopts first
    COMPREPLY=()

    cur="${COMP_WORDS[COMP_CWORD]}"
    prev="${COMP_WORDS[COMP_CWORD-1]}"
    opts="console install start status stop"
    agentServerStorage="--agent --server --storage"
    storageInstallSubopts=" --storage-config --storage-dir "
    agentInstallSubopts=" --agent-config --agent-dir --agent-security-token "
    serverInstallSubopts=" --server-config "

    if [[ "${COMP_LINE}" =~ ^rhqctl[[:space:]]*$ ]] ; then
      COMPREPLY=( $(compgen -W "${opts}" -- ${cur}) )
      return 0
    fi

    first="${COMP_WORDS[1]}"

    if [[ "x${first}" == "xstart" ]] || [[ "x${first}" == "xstop" ]] || [[ "x${first}" == "xstatus" ]] || [[ "x${first}" == "xconsole" ]] ; then
        COMPREPLY=( $(compgen -W "${agentServerStorage}" -- ${cur}) )
        return 0
    elif [[ "x${first}" == "xinstall" ]] ; then
        second="${COMP_WORDS[2]}"
        if [[ "x$second" == "x" ]] ; then
            COMPREPLY=( $(compgen -W "${agentServerStorage}" -- ${cur}) )
            return 0
        fi

        if [[ "x${second}" == "x--server" ]] ; then
            if [[ "${serverInstallSubopts}" == *" ${prev} "* ]] ; then
                # complete path
                COMPREPLY=( $( ls -1 | grep ^${cur} ) )
                return 0
            fi
            COMPREPLY=( $(compgen -W "${serverInstallSubopts}" -- ${cur}) )
            return 0
        elif [[ "x${second}" == "x--agent" ]] ; then
            if [[ "${agentInstallSubopts}" == *" ${prev} "* ]] ; then
                # complete path
                COMPREPLY=( $( ls -1 | grep ^${cur} ) )
                return 0
            fi
            COMPREPLY=( $(compgen -W "${agentInstallSubopts}" -- ${cur}) )
            return 0
        elif [[ "x${second}" == "x--storage" ]] ; then
            if [[ "${storageInstallSubopts}" == *" ${prev} "* ]] ; then
                # complete path
                COMPREPLY=( $( ls -1 | grep ^${cur} ) )
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

complete -F _rhqctl rhqctl
