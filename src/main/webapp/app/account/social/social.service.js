(function() {
    'use strict';

    angular
        .module('sentryApp')
        .factory('SocialService', SocialService);

    SocialService.$inject = ['$http', '$cookies'];

    function SocialService ($http, $cookies) {
        var socialService = {
            getProviderSetting: getProviderSetting,
            getProviderURL: getProviderURL,
            getCSRF: getCSRF
        };

        return socialService;

        function getProviderSetting (provider) {
            switch(provider) {
            case 'google': return 'https://www.googleapis.com/auth/userinfo.profile https://www.googleapis.com/auth/userinfo.email';
            case 'facebook': return 'public_profile,email';
            case 'twitter': return '';
            case 'discord': return 'identify connections guilds guilds.join';
                // jhipster-needle-add-social-button
            default: return 'Provider setting not defined';
            }
        }

        function getProviderURL (provider) {
            return 'signin/' + provider;
        }

        function getCSRF () {
            return $cookies.get($http.defaults.xsrfCookieName);
        }
    }
})();
