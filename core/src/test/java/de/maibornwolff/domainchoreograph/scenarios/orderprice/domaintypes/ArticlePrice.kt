package de.maibornwolff.domainchoreograph.scenarios.orderprice.domaintypes

import de.maibornwolff.domainchoreograph.core.api.DomainDefinition
import de.maibornwolff.domainchoreograph.core.api.DomainFunction

class ArticlePriceNotFoundException : RuntimeException("Article was not found") {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return true
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}

@DomainDefinition
data class ArticlePrice(val value: Float) {
    companion object {
        @DomainFunction
        fun calculate(article: Article, articlePriceService: ArticlePriceService): ArticlePrice {
            val price = articlePriceService.getPrice(article.name) ?: throw ArticlePriceNotFoundException()
            return ArticlePrice(price)
        }
    }
}
