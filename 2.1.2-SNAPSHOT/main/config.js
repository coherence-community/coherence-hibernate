function createConfig() {
    return {
        home: "about/01_overview",
        release: "2.1.2-SNAPSHOT",
        releases: [
            "2.1.2-SNAPSHOT"
        ],
        pathColors: {
            "*": "blue-grey"
        },
        theme: {
            primary: '#1976D2',
            secondary: '#424242',
            accent: '#82B1FF',
            error: '#FF5252',
            info: '#2196F3',
            success: '#4CAF50',
            warning: '#FFC107'
        },
        navTitle: 'Oracle Coherence Hibernate',
        navIcon: null,
        navLogo: 'images/logo.png'
    };
}

function createRoutes(){
    return [
        {
            path: '/about/01_overview',
            meta: {
                h1: 'Overview',
                title: 'Overview',
                h1Prefix: null,
                description: 'Oracle Coherence Hibernate Website',
                keywords: 'coherence, hibernate, java, documentation',
                customLayout: null,
                hasNav: true
            },
            component: loadPage('about-01_overview', '/about/01_overview', {})
        },
        {
            path: '/about/02_hibernate-cache',
            meta: {
                h1: 'Hibernate Cache',
                title: 'Hibernate Cache',
                h1Prefix: null,
                description: null,
                keywords: null,
                customLayout: null,
                hasNav: true
            },
            component: loadPage('about-02_hibernate-cache', '/about/02_hibernate-cache', {})
        },
        {
            path: '/about/03_hibernate-cache-store',
            meta: {
                h1: 'Coherence Hibernate CacheStore',
                title: 'Coherence Hibernate CacheStore',
                h1Prefix: null,
                description: null,
                keywords: null,
                customLayout: null,
                hasNav: true
            },
            component: loadPage('about-03_hibernate-cache-store', '/about/03_hibernate-cache-store', {})
        },
        {
            path: '/dev/01_license',
            meta: {
                h1: 'License',
                title: 'License',
                h1Prefix: null,
                description: 'Oracle Coherence Hibernate License',
                keywords: 'coherence, hibernate, license',
                customLayout: null,
                hasNav: true
            },
            component: loadPage('dev-01_license', '/dev/01_license', {})
        },
        {
            path: '/dev/02_source-code',
            meta: {
                h1: 'Source Code',
                title: 'Source Code',
                h1Prefix: null,
                description: 'Oracle Coherence Hibernate Website',
                keywords: 'coherence, hibernate, java, documentation',
                customLayout: null,
                hasNav: true
            },
            component: loadPage('dev-02_source-code', '/dev/02_source-code', {})
        },
        {
            path: '/dev/03_build-instructions',
            meta: {
                h1: 'Building',
                title: 'Building',
                h1Prefix: null,
                description: 'Oracle Coherence Hibernate Website',
                keywords: 'coherence, hibernate, java, documentation',
                customLayout: null,
                hasNav: true
            },
            component: loadPage('dev-03_build-instructions', '/dev/03_build-instructions', {})
        },
        {
            path: '/dev/04_issue-tracking',
            meta: {
                h1: 'Issue Tracking',
                title: 'Issue Tracking',
                h1Prefix: null,
                description: null,
                keywords: null,
                customLayout: null,
                hasNav: true
            },
            component: loadPage('dev-04_issue-tracking', '/dev/04_issue-tracking', {})
        },
        {
            path: '/dev/05_contributions',
            meta: {
                h1: 'Contributing',
                title: 'Contributing',
                h1Prefix: null,
                description: 'Oracle Coherence Hibernate Website',
                keywords: 'coherence, hibernate, java, documentation',
                customLayout: null,
                hasNav: true
            },
            component: loadPage('dev-05_contributions', '/dev/05_contributions', {})
        },
        {
            path: '/dev/06_history',
            meta: {
                h1: 'Change History',
                title: 'Change History',
                h1Prefix: null,
                description: 'Oracle Coherence Hibernate Website',
                keywords: 'coherence, hibernate, java, documentation',
                customLayout: null,
                hasNav: true
            },
            component: loadPage('dev-06_history', '/dev/06_history', {})
        },
        {
            path: '/dev/07_getting-help',
            meta: {
                h1: 'Getting Help',
                title: 'Getting Help',
                h1Prefix: null,
                description: 'Oracle Coherence Hibernate Website',
                keywords: 'coherence, hibernate, java, documentation',
                customLayout: null,
                hasNav: true
            },
            component: loadPage('dev-07_getting-help', '/dev/07_getting-help', {})
        },
        {
            path: '/', redirect: '/about/01_overview'
        },
        {
            path: '*', redirect: '/'
        }
    ];
}

function createNav(){
    return [
        { header: 'Project Website' },
        {
            title: 'Getting Started',
            action: 'assistant',
            group: '/about',
            items: [
                { href: '/about/01_overview', title: 'Overview' },
                { href: '/about/02_hibernate-cache', title: 'Hibernate Cache' },
                { href: '/about/03_hibernate-cache-store', title: 'Coherence Hibernate CacheStore' }
            ]
        },
        {
            title: 'Development',
            action: 'fa-code',
            group: '/dev',
            items: [
                { href: '/dev/01_license', title: 'License' },
                { href: '/dev/02_source-code', title: 'Source Code' },
                { href: '/dev/03_build-instructions', title: 'Building' },
                { href: '/dev/04_issue-tracking', title: 'Issue Tracking' },
                { href: '/dev/05_contributions', title: 'Contributing' },
                { href: '/dev/06_history', title: 'Change History' },
                { href: '/dev/07_getting-help', title: 'Getting Help' }
            ]
        },
        { divider: true },
        { header: 'Reference Documentation' },
        {
            title: 'Javadocs',
            action: 'code',
            href: 'api/index.html',
            target: '_blank'
        },
        { divider: true },
        { header: 'Additional Resources' },
        {
            title: 'Slack',
            action: 'fa-slack',
            href: 'https://join.slack.com/t/oraclecoherence/shared_invite/enQtNzcxNTQwMTAzNjE4LTJkZWI5ZDkzNGEzOTllZDgwZDU3NGM2YjY5YWYwMzM3ODdkNTU2NmNmNDFhOWIxMDZlNjg2MzE3NmMxZWMxMWE',
            target: '_blank'
        },
        {
            title: 'Coherence Web Site',
            action: 'fa-globe',
            href: 'https://coherence.community/',
            target: '_blank'
        },
        {
            title: 'Coherence Spring',
            action: 'fa-globe',
            href: 'https://spring.coherence.community/',
            target: '_blank'
        },
        {
            title: 'Micronaut Coherence',
            action: 'fa-globe',
            href: 'https://github.com/micronaut-projects/micronaut-coherence/',
            target: '_blank'
        },
        {
            title: 'GitHub',
            action: 'fa-github-square',
            href: 'https://github.com/coherence-community/coherence-hibernate/',
            target: '_blank'
        },
        {
            title: 'Twitter',
            action: 'fa-twitter-square',
            href: 'https://twitter.com/OracleCoherence/',
            target: '_blank'
        }
    ];
}