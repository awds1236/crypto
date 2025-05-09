/**
 * 주식 AI 분석 서비스 JavaScript
 * 주요 거래소 API 데이터와 Claude AI를 이용한 주식 분석 시스템
 */

// 전역 변수
let stockPriceChart = null;
let volumeChart = null;
let indicatorChart = null;
let currentSymbol = null;
let stockMarketType = "US"; // US, KR 등 시장 구분

// DOM이 로드되면 초기화
document.addEventListener('DOMContentLoaded', function() {
    console.log("stock-app.js: DOM이 로드되었습니다.");
    
    // 디버그 모드 설정
    if (typeof window.DEBUG_MODE === 'undefined') {
        window.DEBUG_MODE = true;
    }
    showDebugInfo("주식 분석 스크립트가 초기화되었습니다.");
    
    // 네비게이션 버튼 이벤트 설정
    const navStockBtn = document.getElementById('navStockBtn');
    if (navStockBtn) {
        navStockBtn.addEventListener('click', function() {
            showStockSection();
        });
    } else {
        showDebugInfo("경고: navStockBtn을 찾을 수 없습니다");
    }
    
    // 시장 타입 선택 이벤트
    const marketTypeSelect = document.getElementById('marketTypeSelect');
    if (marketTypeSelect) {
        marketTypeSelect.addEventListener('change', function() {
            stockMarketType = this.value;
            showDebugInfo("시장 타입이 변경되었습니다: " + stockMarketType);
            loadTopVolumeStocks();
        });
    } else {
        showDebugInfo("경고: marketTypeSelect를 찾을 수 없습니다");
    }
    
    // 검색 버튼 이벤트
    const modalStockSearchButton = document.getElementById('modalStockSearchButton');
    if (modalStockSearchButton) {
        modalStockSearchButton.addEventListener('click', function() {
            searchStocksInModal();
        });
    } else {
        showDebugInfo("경고: modalStockSearchButton을 찾을 수 없습니다");
    }
    
    // 검색 입력 필드 엔터 키 이벤트
    const modalStockSearchInput = document.getElementById('modalStockSearchInput');
    if (modalStockSearchInput) {
        modalStockSearchInput.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') {
                searchStocksInModal();
            }
        });
    } else {
        showDebugInfo("경고: modalStockSearchInput을 찾을 수 없습니다");
    }
    
    // 시장 선택 버튼 초기화
    initMarketButtons();
    
    // 상위 종목 로드는 showStockSection()에서 실행됨
});

// 디버그 정보 표시 함수 (중복 방지)
function showDebugInfo(message) {
    if (window.DEBUG_MODE) {
        const debugInfoElement = document.getElementById('debugInfo');
        if (debugInfoElement) {
            debugInfoElement.style.display = 'block';
            // 시간 추가
            const now = new Date();
            const timeStr = now.getHours() + ':' + now.getMinutes() + ':' + now.getSeconds();
            debugInfoElement.innerHTML += '[' + timeStr + '] ' + message + '<br>';
        }
        console.log(message);
    }
}

/**
 * 상위 거래량 주식 로드
 */
function loadTopVolumeStocks() {
    showDebugInfo("상위 거래량 종목 로드 시작...");
    
    const topStocksContainer = document.getElementById('topVolumeStocks');
    if (!topStocksContainer) {
        showDebugInfo("오류: 'topVolumeStocks' 요소를 찾을 수 없습니다");
        return;
    }
    
    // 로딩 인디케이터 표시
    topStocksContainer.innerHTML = `
        <div class="text-center py-3">
            <div class="spinner-border spinner-border-sm" role="status">
                <span class="visually-hidden">Loading...</span>
            </div>
            <span class="ms-2">데이터 로딩 중...</span>
        </div>
    `;
    
    // API 엔드포인트 URL 구성
    const apiUrl = `/api/stock/top-volume?market=${stockMarketType}`;
    showDebugInfo("API 호출: " + apiUrl);
    
    fetch(apiUrl)
        .then(response => {
            showDebugInfo("API 응답 상태: " + response.status);
            if (!response.ok) {
                throw new Error('서버 응답 오류: ' + response.status);
            }
            return response.json();
        })
        .then(data => {
            showDebugInfo("데이터 수신 성공: " + data.length + "개 종목");
            displayTopStocks(data);
        })
        .catch(error => {
            showDebugInfo("오류 발생: " + error.message);
            
            // 에러 메시지 표시
            topStocksContainer.innerHTML = `
                <div class="alert alert-warning m-3">
                    <p>상위 종목 데이터를 불러오지 못했습니다.</p>
                    <p><small>${error.message}</small></p>
                    <button class="btn btn-sm btn-outline-primary mt-2" onclick="loadTopVolumeStocks()">
                        다시 시도
                    </button>
                </div>
            `;
            
            // 데모 데이터로 대체
            const demoStocks = getDefaultStocks();
            setTimeout(() => {
                if (topStocksContainer.querySelector('.alert')) {
                    showDebugInfo("기본 종목 데이터를 사용합니다.");
                    displayTopStocks(demoStocks);
                }
            }, 3000);
        });
}

/**
 * 기본 종목 데이터 생성 (API 연결 실패 시)
 */
function getDefaultStocks() {
    const defaultStocks = [
        {symbol: "AAPL", name: "Apple Inc.", price: 180.25, changePercent: 0.75, exchange: "US", volume: 35000000},
        {symbol: "MSFT", name: "Microsoft Corp.", price: 330.15, changePercent: 1.25, exchange: "US", volume: 28000000},
        {symbol: "GOOGL", name: "Alphabet Inc.", price: 140.35, changePercent: -0.45, exchange: "US", volume: 22000000},
        {symbol: "AMZN", name: "Amazon.com Inc.", price: 132.50, changePercent: 0.35, exchange: "US", volume: 21000000},
        {symbol: "TSLA", name: "Tesla Inc.", price: 240.75, changePercent: -1.25, exchange: "US", volume: 19000000},
        {symbol: "META", name: "Meta Platforms", price: 310.20, changePercent: 2.15, exchange: "US", volume: 17000000},
        {symbol: "NVDA", name: "NVIDIA Corp.", price: 430.45, changePercent: 3.50, exchange: "US", volume: 16000000},
        {symbol: "JPM", name: "JPMorgan Chase", price: 150.70, changePercent: 0.20, exchange: "US", volume: 13000000},
        {symbol: "V", name: "Visa Inc.", price: 240.30, changePercent: -0.15, exchange: "US", volume: 12000000},
        {symbol: "JNJ", name: "Johnson & Johnson", price: 165.45, changePercent: -0.35, exchange: "US", volume: 9500000}
    ];
    return defaultStocks;
}

/**
 * 상위 종목 표시
 */
function displayTopStocks(stocks) {
    const container = document.getElementById('topVolumeStocks');
    if (!container) {
        showDebugInfo("오류: 'topVolumeStocks' 컨테이너를 찾을 수 없습니다");
        return;
    }
    
    if (!stocks || stocks.length === 0) {
        container.innerHTML = '<div class="p-3 text-center text-muted">데이터가 없습니다.</div>';
        return;
    }
    
    let html = '';
    stocks.forEach(stock => {
        // 가격 변동에 따른 클래스 결정
        const priceChangeClass = stock.changePercent > 0 ? 'stock-price-up' : 
                              (stock.changePercent < 0 ? 'stock-price-down' : '');
        
        // 거래량 포맷팅
        const formattedVolume = formatVolume(stock.volume);
        
        html += `
            <div class="top-stock-item p-2 border-bottom" onclick="analyzeStock('${stock.symbol}')">
                <div class="d-flex justify-content-between align-items-center">
                    <div>
                        <strong>${stock.name}</strong>
                        <small class="text-muted ms-2">${stock.symbol}</small>
                        <span class="market-indicator ${stock.exchange}">${stock.exchange}</span>
                    </div>
                    <div class="text-end">
                        <div class="${priceChangeClass}">$${Number(stock.price).toFixed(2)}</div>
                        <small class="${priceChangeClass}">${stock.changePercent > 0 ? '+' : ''}${Number(stock.changePercent).toFixed(2)}%</small>
                        <div class="text-muted small">${formattedVolume}</div>
                    </div>
                </div>
            </div>
        `;
    });
    
    container.innerHTML = html;
    showDebugInfo("종목 목록 표시 완료");
}

/**
 * 종목 검색
 */
function searchStocks() {
    const searchInput = document.getElementById('stockSearchInput');
    if (!searchInput || !searchInput.value.trim()) {
        alert('검색어를 입력해주세요');
        return;
    }
    
    const query = searchInput.value.trim();
    showDebugInfo("종목 검색: " + query);
    
    const resultsContainer = document.getElementById('searchResultsContainer');
    if (!resultsContainer) {
        showDebugInfo("오류: 'searchResultsContainer'를 찾을 수 없습니다");
        return;
    }
    
    // 로딩 표시
    resultsContainer.innerHTML = `
        <div class="text-center py-2">
            <div class="spinner-border spinner-border-sm" role="status">
                <span class="visually-hidden">Loading...</span>
            </div>
            <span class="ms-2">검색 중...</span>
        </div>
    `;
    
    const apiUrl = `/api/stock/search?query=${encodeURIComponent(query)}&market=${stockMarketType}`;
    showDebugInfo("검색 API 호출: " + apiUrl);
    
    fetch(apiUrl)
        .then(response => {
            showDebugInfo("검색 응답 상태: " + response.status);
            if (!response.ok) {
                throw new Error('서버 응답 오류: ' + response.status);
            }
            return response.json();
        })
        .then(data => {
            showDebugInfo("검색 결과: " + data.length + "개 종목");
            displaySearchResults(data);
        })
        .catch(error => {
            showDebugInfo("검색 오류: " + error.message);
            
            resultsContainer.innerHTML = `
                <div class="alert alert-warning">
                    <p>검색에 실패했습니다.</p>
                    <p><small>${error.message}</small></p>
                </div>
            `;
            
            // 간단한 데모 결과 표시 (예시)
            setTimeout(() => {
                if (resultsContainer.querySelector('.alert')) {
                    const demoResults = getDemoSearchResults(query);
                    displaySearchResults(demoResults);
                }
            }, 2000);
        });
}

/**
 * 데모 검색 결과 생성
 */
function getDemoSearchResults(query) {
    query = query.toLowerCase();
    const results = [];
    
    // 간단한 키워드 매칭 (실제 서비스에서는 서버 측 검색 사용)
    const allStocks = getDefaultStocks();
    
    for (const stock of allStocks) {
        if (stock.name.toLowerCase().includes(query) || 
            stock.symbol.toLowerCase().includes(query)) {
            results.push(stock);
        }
    }
    
    return results;
}

/**
 * 검색 결과 표시
 */
function displaySearchResults(results) {
    const container = document.getElementById('searchResultsContainer');
    if (!container) return;
    
    if (!results || results.length === 0) {
        container.innerHTML = '<div class="alert alert-info">검색 결과가 없습니다.</div>';
        return;
    }
    
    let html = '<div class="list-group mt-2">';
    results.forEach(stock => {
        const priceChangeClass = stock.changePercent > 0 ? 'stock-price-up' : 
                              (stock.changePercent < 0 ? 'stock-price-down' : '');
        
        html += `
            <a href="javascript:void(0)" class="list-group-item list-group-item-action" onclick="analyzeStock('${stock.symbol}')">
                <div class="d-flex justify-content-between align-items-center">
                    <div>
                        <strong>${stock.name}</strong>
                        <small class="text-muted ms-2">${stock.symbol}</small>
                    </div>
                    <div class="text-end ${priceChangeClass}">
                        ${stock.price ? '$' + Number(stock.price).toFixed(2) : ''}
                    </div>
                </div>
            </a>
        `;
    });
    html += '</div>';
    
    container.innerHTML = html;
    showDebugInfo("검색 결과 표시 완료");
}

/**
 * 주식 분석 실행
 * @param {string} symbol - 종목 심볼 (예: AAPL, MSFT)
 */
function analyzeStock(symbol) {
    if (!symbol) {
        alert('분석할 종목을 선택해주세요');
        return;
    }
    
    currentSymbol = symbol;
    showDebugInfo("종목 분석 시작: " + symbol);
    
    // 로딩 표시
    const loadingIndicator = document.getElementById('stock-loading');
    const resultsPanel = document.getElementById('stock-analysis-result');
    const selectionPanel = document.getElementById('stockSelectionPanel');
    
    if (loadingIndicator) loadingIndicator.style.display = 'block';
    if (resultsPanel) resultsPanel.style.display = 'none';
    if (selectionPanel) selectionPanel.style.display = 'none';
    
    // API 요청
    const apiUrl = `/api/stock/analyze?symbol=${symbol}&market=${stockMarketType}`;
    showDebugInfo("분석 API 호출: " + apiUrl);
    
    fetch(apiUrl)
        .then(response => {
            showDebugInfo("분석 응답 상태: " + response.status);
            if (!response.ok) {
                throw new Error('서버 응답 오류: ' + response.status);
            }
            return response.json();
        })
        .then(data => {
            showDebugInfo("분석 데이터 수신 성공");
            displayAnalysisResults(data);
        })
        .catch(error => {
            showDebugInfo("분석 오류: " + error.message);
            
            if (loadingIndicator) loadingIndicator.style.display = 'none';
            if (selectionPanel) selectionPanel.style.display = 'block';
            
            // 오류 알림
            showError(`종목 분석 중 오류가 발생했습니다: ${error.message}`);
            
            // 간단한 데모 분석 결과로 대체 (옵션)
            const demoResults = getDemoAnalysisResults(symbol);
            setTimeout(() => {
                displayAnalysisResults(demoResults);
            }, 500);
        });
}

/**
 * 데모 분석 결과 생성
 */
function getDemoAnalysisResults(symbol) {
    // 기본 주식 찾기
    const defaultStocks = getDefaultStocks();
    const stockInfo = defaultStocks.find(s => s.symbol === symbol) || {
        symbol: symbol,
        name: `${symbol} Corporation`,
        price: 100 + Math.random() * 100,
        changePercent: Math.random() * 4 - 2 // -2% ~ 2%
    };
    
    return {
        success: true,
        symbol: stockInfo.symbol,
        companyName: stockInfo.name,
        currentPrice: stockInfo.price,
        analysis: `
            <h4>${stockInfo.name} (${stockInfo.symbol}) 분석</h4>
            <p>이 분석은 데모 데이터입니다. API 연결이 필요합니다.</p>
            <p>현재 ${stockInfo.symbol}는 ${stockInfo.changePercent > 0 ? '상승' : '하락'} 추세에 있으며, 
            기술적 지표상으로는 ${Math.random() > 0.5 ? '매수' : '관망'} 신호를 보이고 있습니다.</p>
            <p>주요 지지선은 $${(stockInfo.price * 0.95).toFixed(2)}와 $${(stockInfo.price * 0.9).toFixed(2)}에 형성되어 있으며,
            저항선은 $${(stockInfo.price * 1.05).toFixed(2)}와 $${(stockInfo.price * 1.1).toFixed(2)}에 있습니다.</p>
        `,
        indicators: {
            latest: {
                sma20: stockInfo.price * 0.98,
                sma50: stockInfo.price * 0.96,
                sma200: stockInfo.price * 0.92,
                rsi: 40 + Math.random() * 20
            },
            sma20: generateDummyArray(stockInfo.price * 0.95, stockInfo.price * 1.05, 30),
            sma50: generateDummyArray(stockInfo.price * 0.9, stockInfo.price * 1.1, 30),
            rsi: generateDummyArray(30, 70, 30)
        },
        financials: {
            marketCap: stockInfo.price * 1000000000,
            pe: 15 + Math.random() * 10,
            eps: stockInfo.price / 20,
            dividend: Math.random() * 2,
            beta: 0.8 + Math.random() * 0.8,
            sector: "Technology",
            industry: "Software"
        },
        news: {
            articlesCount: 3,
            articles: [
                {
                    title: `${stockInfo.name} 신규 제품 출시 예정`,
                    publishedAt: new Date().toISOString(),
                    source: "Finance News",
                    url: "#"
                },
                {
                    title: `분기 실적 예상치 상회, ${stockInfo.symbol} 주가 반등`,
                    publishedAt: new Date().toISOString(),
                    source: "Market Watch",
                    url: "#"
                },
                {
                    title: `${stockInfo.symbol}의 신규 시장 진출 계획 발표`,
                    publishedAt: new Date().toISOString(),
                    source: "Business Insider",
                    url: "#"
                }
            ]
        },
        dates: Array.from({length: 30}, (_, i) => {
            const date = new Date();
            date.setDate(date.getDate() - (30 - i));
            return `${date.getMonth() + 1}/${date.getDate()}`;
        }),
        prices: generateDummyArray(stockInfo.price * 0.9, stockInfo.price * 1.1, 30),
        volumes: Array.from({length: 30}, () => Math.floor(Math.random() * 10000000) + 5000000)
    };
}

/**
 * 더미 데이터 배열 생성
 */
function generateDummyArray(min, max, length) {
    return Array.from({length}, () => min + Math.random() * (max - min));
}

/**
 * 분석 결과 표시
 */
function displayAnalysisResults(data) {
    const loadingIndicator = document.getElementById('stock-loading');
    const resultsPanel = document.getElementById('stock-analysis-result');
    const selectionPanel = document.getElementById('stockSelectionPanel');
    
    if (loadingIndicator) loadingIndicator.style.display = 'none';
    if (selectionPanel) selectionPanel.style.display = 'none';
    
    if (!data.success) {
        showError("분석 결과를 가져오지 못했습니다: " + (data.error || "알 수 없는 오류"));
        if (selectionPanel) selectionPanel.style.display = 'block';
        return;
    }
    
    if (resultsPanel) {
        resultsPanel.style.display = 'block';
        
        // 종목 정보 업데이트
        updateStockInfo(data);
        
        // AI 분석 텍스트 업데이트
        document.getElementById('aiAnalysisText').innerHTML = data.analysis;
        
        // 차트 그리기
        drawStockCharts(data);
        
        // 재무 정보 업데이트
        updateFinancialInfo(data.financials);
        
        // 뉴스 업데이트
        updateStockNews(data.news);
        
        // 페이지 타이틀 업데이트
        updateStockPageTitle(data.symbol, data.companyName, data.currentPrice);
    } else {
        showDebugInfo("오류: 'stock-analysis-result' 요소를 찾을 수 없습니다");
    }
}

/**
 * 종목 정보 헤더 업데이트
 */
function updateStockInfo(data) {
    // 종목명, 심볼
    document.getElementById('stockName').textContent = data.companyName || "N/A";
    document.getElementById('stockSymbol').textContent = `(${data.symbol})`;
    
    // 시장 표시
    const marketBadge = document.getElementById('marketBadge');
    if (marketBadge) {
        marketBadge.textContent = stockMarketType;
        marketBadge.className = `market-indicator ${stockMarketType}`;
    }
    
    // 현재가
    document.getElementById('currentPrice').textContent = `$${Number(data.currentPrice).toFixed(2)}`;
    
    // 변동폭
    const priceChange = document.getElementById('priceChange');
    if (data.changePercent !== undefined) {
        const changeClass = data.changePercent > 0 ? 'stock-price-up' : 
                          (data.changePercent < 0 ? 'stock-price-down' : '');
        
        priceChange.className = changeClass;
        priceChange.textContent = `${data.changePercent > 0 ? '+' : ''}${data.changePercent.toFixed(2)}%`;
    } else {
        priceChange.textContent = "N/A";
    }
    
    // 업데이트 시간
    document.getElementById('updateTime').textContent = "최종 업데이트: " + new Date().toLocaleString();
}

/**
 * 주식 차트 그리기
 */
function drawStockCharts(data) {
    // 가격 차트 그리기
    drawStockPriceChart(data);
    
    // RSI 차트 그리기
    drawRsiChart(data.indicators.rsi);
    
    // MACD 차트 그리기
    // ...

    showDebugInfo("차트 그리기 완료");
}

/**
 * 주식 가격 차트 그리기
 */
function drawStockPriceChart(data) {
    const ctx = document.getElementById('stockPriceChart').getContext('2d');
    
    // 기존 차트 제거
    if (stockPriceChart) {
        stockPriceChart.destroy();
    }
    
    // 데이터 확인
    if (!data.prices || !data.dates) {
        showDebugInfo("가격 차트 데이터가 없습니다");
        return;
    }
    
    // 데이터 준비
    const chartData = {
        labels: data.dates,
        datasets: [
            {
                label: '종가',
                data: data.prices,
                borderColor: 'rgba(54, 162, 235, 1)',
                backgroundColor: 'rgba(54, 162, 235, 0.1)',
                borderWidth: 2,
                fill: false
            }
        ]
    };
    
    // SMA 추가 (있는 경우)
    if (data.indicators && data.indicators.sma50) {
        chartData.datasets.push({
            label: 'SMA 50',
            data: data.indicators.sma50,
            borderColor: 'rgba(255, 99, 132, 1)',
            backgroundColor: 'rgba(255, 99, 132, 0.1)',
            borderWidth: 1.5,
            fill: false
        });
    }
    
    // 차트 생성
    stockPriceChart = new Chart(ctx, {
        type: 'line',
        data: chartData,
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
                x: {
                    display: true,
                    title: {
                        display: true,
                        text: '날짜'
                    }
                },
                y: {
                    display: true,
                    title: {
                        display: true,
                        text: '가격 (USD)'
                    }
                }
            },
            plugins: {
                tooltip: {
                    mode: 'index',
                    intersect: false,
                },
                legend: {
                    position: 'top',
                },
                title: {
                    display: true,
                    text: `${data.companyName} (${data.symbol}) 가격 차트`
                }
            },
            interaction: {
                mode: 'nearest',
                axis: 'x',
                intersect: false
            },
            elements: {
                line: {
                    tension: 0.4 // 부드러운 곡선
                }
            }
        }
    });
}

/**
 * RSI 차트 그리기
 */
function drawRsiChart(rsiData) {
    const ctx = document.getElementById('stockRsiChart').getContext('2d');
    
    // 기존 차트 제거
    if (indicatorChart) {
        indicatorChart.destroy();
    }
    
    if (!rsiData) {
        showDebugInfo("RSI 차트 데이터가 없습니다");
        return;
    }
    
    // 차트 생성
    indicatorChart = new Chart(ctx, {
        type: 'line',
        data: {
            labels: Array.from({length: rsiData.length}, (_, i) => i + 1),
            datasets: [{
                label: 'RSI (14)',
                data: rsiData,
                borderColor: 'rgba(153, 102, 255, 1)',
                backgroundColor: 'rgba(153, 102, 255, 0.1)',
                borderWidth: 2,
                fill: false
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
                y: {
                    min: 0,
                    max: 100,
                    grid: {
                        color: function(context) {
                            if (context.tick.value === 30 || context.tick.value === 70) {
                                return 'rgba(255, 0, 0, 0.5)';
                            }
                            return 'rgba(0, 0, 0, 0.1)';
                        }
                    }
                }
            },
            plugins: {
                tooltip: {
                    callbacks: {
                        label: function(context) {
                            const value = context.parsed.y;
                            let status = '';
                            
                            if (value > 70) status = ' (과매수)';
                            else if (value < 30) status = ' (과매도)';
                            
                            return `RSI: ${value.toFixed(2)}${status}`;
                        }
                    }
                }
            }
        }
    });
}

/**
 * 재무 정보 업데이트
 */
function updateFinancialInfo(financials) {
    if (!financials) return;
    
    // 업종, 산업 정보
    document.getElementById('sectorInfo').textContent = financials.sector || "-";
    document.getElementById('industryInfo').textContent = financials.industry || "-";
    
    // 시가총액
    document.getElementById('marketCapInfo').textContent = 
        financials.marketCap ? formatCurrency(financials.marketCap) : "-";
    
    // 주가 지표
    document.getElementById('peRatioInfo').textContent = 
        financials.pe ? financials.pe.toFixed(2) : "-";
    document.getElementById('epsInfo').textContent = 
        financials.eps ? formatCurrency(financials.eps) : "-";
    document.getElementById('dividendYieldInfo').textContent = 
        financials.dividend ? financials.dividend.toFixed(2) + "%" : "-";
    document.getElementById('betaInfo').textContent = 
        financials.beta ? financials.beta.toFixed(2) : "-";
    document.getElementById('roeInfo').textContent = 
        financials.roe ? financials.roe.toFixed(2) + "%" : "-";
    
    // 회사 설명
    document.getElementById('companyDescription').textContent = 
        financials.description || `${currentSymbol}에 대한 자세한 정보가 없습니다.`;
}

/**
 * 주식 뉴스 업데이트
 */
function updateStockNews(newsData) {
    const container = document.getElementById('stockNewsContainer');
    if (!container) return;
    
    if (!newsData || !newsData.articles || newsData.articles.length === 0) {
        container.innerHTML = '<p class="text-center py-3 text-muted">관련 뉴스가 없습니다.</p>';
        return;
    }
    
    let html = '';
    
    newsData.articles.forEach(news => {
        // 날짜 포맷팅
        let publishDate = '';
        try {
            publishDate = new Date(news.publishedAt).toLocaleString();
        } catch (e) {
            publishDate = news.publishedAt || '';
        }
        
        html += `
            <div class="news-item p-3">
                <h5 class="news-title">${news.title}</h5>
                <div class="d-flex justify-content-between align-items-center mb-2">
                    <small class="news-date text-muted">${publishDate}</small>
                    <span class="badge bg-light text-dark">${news.source}</span>
                </div>
                ${news.summary ? `<p>${news.summary}</p>` : ''}
                <a href="${news.url}" target="_blank" class="btn btn-sm btn-outline-primary mt-2">뉴스 보기</a>
            </div>
        `;
    });
    
    container.innerHTML = html;
}

/**
 * 페이지 타이틀 업데이트
 */
function updateStockPageTitle(symbol, companyName, price) {
    if (price) {
        document.title = `$${Number(price).toFixed(2)} | ${companyName} (${symbol}) - 주식 AI 분석`;
    } else {
        document.title = `${companyName} (${symbol}) - 주식 AI 분석`;
    }
}

/**
 * 통화 포맷팅
 */
function formatCurrency(value) {
    return new Intl.NumberFormat('en-US', {
        style: 'currency',
        currency: 'USD',
        minimumFractionDigits: 2
    }).format(value);
}

/**
 * 거래량 포맷팅
 */
function formatVolume(value) {
    return new Intl.NumberFormat('en-US').format(value);
}

/**
 * 거래량 축약 포맷팅 (차트용)
 */
function formatVolumeShort(value) {
    if (value >= 1000000000) {
        return (value / 1000000000).toFixed(1) + 'B';
    } else if (value >= 1000000) {
        return (value / 1000000).toFixed(1) + 'M';
    } else if (value >= 1000) {
        return (value / 1000).toFixed(1) + 'K';
    }
    return value;
}

/**
 * 오류 메시지 표시
 */
function showError(message) {
    showDebugInfo("오류 발생: " + message);
    
    // 알림 요소 생성
    const alertDiv = document.createElement('div');
    alertDiv.className = 'alert alert-danger alert-dismissible fade show mt-3';
    alertDiv.innerHTML = `
        ${message}
        <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
    `;
    
    // 컨테이너에 알림 추가
    const container = document.querySelector('.container');
    container.insertBefore(alertDiv, container.firstChild);
    
    // 5초 후 자동 제거
    setTimeout(() => {
        alertDiv.classList.remove('show');
        setTimeout(() => alertDiv.remove(), 500);
    }, 5000);
}

/**
 * 시장 선택 버튼 초기화
 */
function initMarketButtons() {
    console.log("시장 선택 버튼 초기화");
    
    const usMarketBtn = document.getElementById('usMarketBtn');
    const krMarketBtn = document.getElementById('krMarketBtn');
    
    if (usMarketBtn && krMarketBtn) {
        // 초기 상태 설정
        if (stockMarketType === 'US') {
            usMarketBtn.classList.add('active');
            krMarketBtn.classList.remove('active');
        } else {
            krMarketBtn.classList.add('active');
            usMarketBtn.classList.remove('active');
        }
        
        // 미국 시장 버튼 클릭 이벤트
        usMarketBtn.addEventListener('click', function() {
            if (!this.classList.contains('active')) {
                krMarketBtn.classList.remove('active');
                this.classList.add('active');
                stockMarketType = 'US';
                showDebugInfo("시장 변경: US");
                updatePopularStocks('US');
            }
        });
        
        // 한국 시장 버튼 클릭 이벤트
        krMarketBtn.addEventListener('click', function() {
            if (!this.classList.contains('active')) {
                usMarketBtn.classList.remove('active');
                this.classList.add('active');
                stockMarketType = 'KR';
                showDebugInfo("시장 변경: KR");
                updatePopularStocks('KR');
            }
        });
        
        // 초기 인기 종목 로드
        updatePopularStocks(stockMarketType);
    } else {
        showDebugInfo("경고: 시장 선택 버튼을 찾을 수 없습니다");
    }
}

/**
 * 시장별 인기 종목 업데이트
 */
function updatePopularStocks(market) {
    const container = document.getElementById('popularStocks');
    if (!container) {
        showDebugInfo("경고: popularStocks 컨테이너를 찾을 수 없습니다");
        return;
    }
    
    // 로딩 표시
    container.innerHTML = `
        <div class="text-center py-3">
            <div class="spinner-border spinner-border-sm text-primary" role="status">
                <span class="visually-hidden">Loading...</span>
            </div>
            <span class="ms-2">인기 종목 로딩 중...</span>
        </div>
    `;
    
    // API 호출 (실제로는 서버에서 인기 종목 가져오기)
    const apiUrl = `/api/stock/popular?market=${market}`;
    showDebugInfo("인기 종목 API 호출: " + apiUrl);
    
    fetch(apiUrl)
        .then(response => {
            if (!response.ok) {
                throw new Error('서버 응답 오류: ' + response.status);
            }
            return response.json();
        })
        .then(data => {
            showDebugInfo("인기 종목 데이터 수신: " + data.length + "개");
            displayPopularStocks(data, market);
        })
        .catch(error => {
            showDebugInfo("인기 종목 로드 오류: " + error.message);
            
            // 데모 데이터 사용
            setTimeout(() => {
                const popularStocks = getDefaultPopularStocks(market);
                displayPopularStocks(popularStocks, market);
            }, 500);
        });
}

/**
 * 기본 인기 종목 데이터 (API 실패 시)
 */
function getDefaultPopularStocks(market) {
    return market === 'US' ? 
        [
            {symbol: "AAPL", name: "Apple Inc.", changePercent: 0.75},
            {symbol: "MSFT", name: "Microsoft Corp.", changePercent: 1.25},
            {symbol: "GOOGL", name: "Alphabet Inc.", changePercent: -0.45},
            {symbol: "AMZN", name: "Amazon.com Inc.", changePercent: 0.35},
            {symbol: "TSLA", name: "Tesla Inc.", changePercent: -1.25}
        ] : 
        [
            {symbol: "005930", name: "삼성전자", changePercent: 0.5},
            {symbol: "000660", name: "SK하이닉스", changePercent: 1.1},
            {symbol: "373220", name: "LG에너지솔루션", changePercent: -0.7},
            {symbol: "005380", name: "현대자동차", changePercent: 0.2},
            {symbol: "035420", name: "NAVER", changePercent: -0.3}
        ];
}

/**
 * 인기 종목 표시
 */
function displayPopularStocks(stocks, market) {
    const container = document.getElementById('popularStocks');
    if (!container) return;
    
    if (!stocks || stocks.length === 0) {
        container.innerHTML = '<div class="text-center py-3 text-muted">인기 종목이 없습니다.</div>';
        return;
    }
    
    let html = '';
    stocks.forEach(stock => {
        // 가격 변동에 따른 클래스
        const priceChangeClass = stock.changePercent > 0 ? 'stock-price-up' : 
                               (stock.changePercent < 0 ? 'stock-price-down' : '');
        
        html += `
            <a href="javascript:void(0)" class="list-group-item list-group-item-action" 
               onclick="analyzeStock('${stock.symbol}')">
                <div class="d-flex justify-content-between align-items-center">
                    <div>
                        <strong>${stock.name}</strong>
                        <small class="text-muted ms-2">${stock.symbol}</small>
                        <span class="market-indicator ${market}">${market}</span>
                    </div>
                    ${stock.changePercent ? `
                    <div class="${priceChangeClass}">
                        ${stock.changePercent > 0 ? '+' : ''}${stock.changePercent.toFixed(2)}%
                    </div>
                    ` : ''}
                </div>
            </a>
        `;
    });
    
    container.innerHTML = html;
}

/**
 * 모달 내 종목 검색
 */
function searchStocksInModal() {
    const searchInput = document.getElementById('modalStockSearchInput');
    if (!searchInput || !searchInput.value.trim()) {
        alert('검색어를 입력해주세요');
        return;
    }
    
    const query = searchInput.value.trim();
    showDebugInfo("종목 검색: " + query);
    
    const resultsContainer = document.getElementById('modalSearchResultsContainer');
    if (!resultsContainer) {
        showDebugInfo("오류: 'modalSearchResultsContainer'를 찾을 수 없습니다");
        return;
    }
    
    // 로딩 표시
    resultsContainer.innerHTML = `
        <div class="text-center py-2">
            <div class="spinner-border spinner-border-sm" role="status">
                <span class="visually-hidden">Loading...</span>
            </div>
            <span class="ms-2">검색 중...</span>
        </div>
    `;
    
    const apiUrl = `/api/stock/search?query=${encodeURIComponent(query)}&market=${stockMarketType}`;
    showDebugInfo("검색 API 호출: " + apiUrl);
    
    fetch(apiUrl)
        .then(response => {
            showDebugInfo("검색 응답 상태: " + response.status);
            if (!response.ok) {
                throw new Error('서버 응답 오류: ' + response.status);
            }
            return response.json();
        })
        .then(data => {
            showDebugInfo("검색 결과: " + data.length + "개 종목");
            displayModalSearchResults(data);
        })
        .catch(error => {
            showDebugInfo("검색 오류: " + error.message);
            
            // 오류 메시지 표시
            resultsContainer.innerHTML = `
                <div class="alert alert-warning">
                    <p>검색에 실패했습니다.</p>
                    <p><small>${error.message}</small></p>
                </div>
            `;
            
            // 데모 결과 표시 (API 실패 시)
            setTimeout(() => {
                if (resultsContainer.querySelector('.alert')) {
                    const demoResults = getDemoSearchResults(query);
                    displayModalSearchResults(demoResults);
                }
            }, 1000);
        });
}

/**
 * 데모 검색 결과 생성 (API 실패 시)
 */
function getDemoSearchResults(query) {
    query = query.toLowerCase();
    const results = [];
    
    // 현재 선택된 시장에 따라 다른 데모 데이터 제공
    const demoStocks = stockMarketType === 'US' ? [
        {symbol: "AAPL", name: "Apple Inc.", price: 180.25, changePercent: 0.75, exchange: "US"},
        {symbol: "MSFT", name: "Microsoft Corp.", price: 330.15, changePercent: 1.25, exchange: "US"},
        {symbol: "GOOGL", name: "Alphabet Inc.", price: 140.35, changePercent: -0.45, exchange: "US"},
        {symbol: "AMZN", name: "Amazon.com Inc.", price: 132.50, changePercent: 0.35, exchange: "US"},
        {symbol: "TSLA", name: "Tesla Inc.", price: 240.75, changePercent: -1.25, exchange: "US"},
        {symbol: "META", name: "Meta Platforms", price: 310.20, changePercent: 2.15, exchange: "US"},
        {symbol: "NVDA", name: "NVIDIA Corp.", price: 430.45, changePercent: 3.50, exchange: "US"},
        {symbol: "JPM", name: "JPMorgan Chase", price: 150.70, changePercent: 0.20, exchange: "US"},
        {symbol: "V", name: "Visa Inc.", price: 240.30, changePercent: -0.15, exchange: "US"},
        {symbol: "JNJ", name: "Johnson & Johnson", price: 165.45, changePercent: -0.35, exchange: "US"}
    ] : [
        {symbol: "005930", name: "삼성전자", price: 71200, changePercent: 0.5, exchange: "KR"},
        {symbol: "000660", name: "SK하이닉스", price: 137500, changePercent: 1.1, exchange: "KR"},
        {symbol: "373220", name: "LG에너지솔루션", price: 410000, changePercent: -0.7, exchange: "KR"},
        {symbol: "005380", name: "현대자동차", price: 210000, changePercent: 0.2, exchange: "KR"},
        {symbol: "035420", name: "NAVER", price: 192500, changePercent: -0.3, exchange: "KR"},
        {symbol: "051910", name: "LG화학", price: 510000, changePercent: 0.8, exchange: "KR"},
        {symbol: "207940", name: "삼성바이오로직스", price: 815000, changePercent: -0.4, exchange: "KR"},
        {symbol: "035720", name: "카카오", price: 51900, changePercent: 0.1, exchange: "KR"},
        {symbol: "105560", name: "KB금융", price: 63400, changePercent: 0.6, exchange: "KR"},
        {symbol: "055550", name: "신한지주", price: 41850, changePercent: 0.3, exchange: "KR"}
    ];
    
    // 간단한 키워드 매칭 (실제 서비스에서는 서버 측 검색 사용)
    for (const stock of demoStocks) {
        if (stock.name.toLowerCase().includes(query) || 
            stock.symbol.toLowerCase().includes(query)) {
            results.push(stock);
        }
    }
    
    // 검색어가 없으면 모든 결과 반환 (최대 5개)
    if (results.length === 0 && query.length === 0) {
        return demoStocks.slice(0, 5);
    }
    
    return results;
}

/**
 * 모달 내 검색 결과 표시
 */
function displayModalSearchResults(results) {
    const container = document.getElementById('modalSearchResultsContainer');
    if (!container) return;
    
    if (!results || results.length === 0) {
        container.innerHTML = '<div class="alert alert-info">검색 결과가 없습니다.</div>';
        return;
    }
    
    let html = '<div class="list-group mt-2">';
    results.forEach(stock => {
        const priceChangeClass = stock.changePercent > 0 ? 'stock-price-up' : 
                             (stock.changePercent < 0 ? 'stock-price-down' : '');
        
        html += `
            <a href="javascript:void(0)" class="list-group-item list-group-item-action" 
               onclick="analyzeStock('${stock.symbol}')">
                <div class="d-flex justify-content-between align-items-center">
                    <div>
                        <strong>${stock.name}</strong>
                        <small class="text-muted ms-2">${stock.symbol}</small>
                        <span class="market-indicator ${stock.exchange || stockMarketType}">${stock.exchange || stockMarketType}</span>
                    </div>
                    ${stock.price ? `
                    <div class="text-end ${priceChangeClass}">
                        ${stockMarketType === 'US' ? '$' : '₩'}${Number(stock.price).toFixed(2)}
                        ${stock.changePercent ? `<small class="${priceChangeClass}">(${stock.changePercent > 0 ? '+' : ''}${stock.changePercent.toFixed(2)}%)</small>` : ''}
                    </div>
                    ` : ''}
                </div>
            </a>
        `;
    });
    html += '</div>';
    
    container.innerHTML = html;
    showDebugInfo("검색 결과 표시 완료");
}