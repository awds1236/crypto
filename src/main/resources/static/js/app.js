/**
 * 가상화폐 AI 분석 서비스 JavaScript
 * 업비트, 바이낸스 API 데이터와 Claude AI를 이용한 가상화폐 분석 시스템
 */

// 전역 변수
let priceChart = null;
let rsiChart = null;
let stompClient = null;
let currentMarket = null;
let currentExchange = "upbit"; // 기본 거래소 설정
let realTimePrices = {};

// DOM이 로드되면 초기화
document.addEventListener('DOMContentLoaded', function() {
    // 초기 거래소에 따른 코인 목록 로드
    loadMarkets(currentExchange);
    
    // 거래소 변경 이벤트
    document.getElementById('exchangeSelect').addEventListener('change', function() {
        currentExchange = this.value;
        loadMarkets(currentExchange);
    });
    
    // 분석 버튼 이벤트
    document.getElementById('analyzeBtn').addEventListener('click', function() {
        const market = document.getElementById('marketSelect').value;
        if (market) {
            currentMarket = market;
            analyzeMarket(market, currentExchange);
            
            // 알림 권한 요청 (선택 사항)
            requestNotificationPermission();
        } else {
            alert('코인을 선택해주세요');
        }
    });
});

/**
 * 선택한 거래소에 따른 코인 목록 로드
 * @param {string} exchange - 거래소 코드 (upbit 또는 binance)
 */
function loadMarkets(exchange) {
    console.log(`Loading markets from ${exchange}...`);
    
    // 로딩 중 표시 추가
    const select = document.getElementById('marketSelect');
    select.innerHTML = '<option value="">코인 목록 로딩 중...</option>';
    select.disabled = true;
    
    fetch(`/markets?exchange=${exchange}`)
        .then(response => {
            console.log("Response status:", response.status);
            
            if (!response.ok) {
                throw new Error('서버 응답 오류: ' + response.status);
            }
            
            return response.json();
        })
        .then(data => {
            console.log(`${exchange} markets data received:`, data.length, "markets");
            
            select.disabled = false;
            select.innerHTML = ''; // 로딩 메시지 제거
            
            // 안내 옵션 추가
            const defaultOption = document.createElement('option');
            defaultOption.value = "";
            defaultOption.textContent = "코인을 선택하세요";
            select.appendChild(defaultOption);
            
            // 거래소별 필터링 및 정렬
            let filteredMarkets = [];
            
            if (exchange === "upbit") {
                // 업비트: KRW 마켓만 필터링
                filteredMarkets = data.filter(market => market.market && market.market.startsWith('KRW-'));
                
                // 한국어 이름 기준 정렬
                filteredMarkets.sort((a, b) => {
                    const aName = a.korean_name || a.market;
                    const bName = b.korean_name || b.market;
                    return aName.localeCompare(bName);
                });
                
                // 옵션 생성
                filteredMarkets.forEach(market => {
                    const option = document.createElement('option');
                    option.value = market.market;
                    
                    const koreanName = market.korean_name || '이름 없음';
                    option.textContent = `${koreanName} (${market.market})`;
                    
                    select.appendChild(option);
                });
            } else if (exchange === "binance") {
                // 바이낸스: USDT 마켓 (이미 필터링됨)
                filteredMarkets = data;
                
                // 영어 심볼 기준 정렬
                filteredMarkets.sort((a, b) => {
                    const aSymbol = a.baseAsset || "";
                    const bSymbol = b.baseAsset || "";
                    return aSymbol.localeCompare(bSymbol);
                });
                
                // 옵션 생성
                filteredMarkets.forEach(market => {
                    const option = document.createElement('option');
                    option.value = market.market;
                    
                    const koreanName = market.korean_name || market.baseAsset;
                    option.textContent = `${koreanName} (${market.market})`;
                    
                    select.appendChild(option);
                });
            }
            
            console.log(`Filtered ${exchange} markets:`, filteredMarkets.length);
            
            // 주요 코인 자동 선택 (거래소별)
            if (exchange === "upbit") {
                const btcOption = Array.from(select.options).find(option => option.value === 'KRW-BTC');
                if (btcOption) {
                    btcOption.selected = true;
                }
            } else if (exchange === "binance") {
                const btcOption = Array.from(select.options).find(option => option.value === 'BTCUSDT');
                if (btcOption) {
                    btcOption.selected = true;
                }
            }
            
            // 선택된 옵션이 없으면 첫 번째 실제 코인 선택
            if (select.selectedIndex === 0 && select.options.length > 1) {
                select.selectedIndex = 1;
            }
        })
        .catch(error => {
            console.error(`${exchange} 코인 목록 로드 실패:`, error);
            
            // 로딩 상태 해제
            select.disabled = false;
            select.innerHTML = ''; // 기존 옵션 제거
            
            // 안내 옵션 추가
            const errorOption = document.createElement('option');
            errorOption.value = "";
            errorOption.textContent = "코인 목록을 불러오지 못했습니다";
            select.appendChild(errorOption);
            
            // 기본 코인 목록 추가 (API 실패 시에도 사용 가능하도록)
            const defaultCoins = exchange === "upbit" ? 
                [
                    {value: "KRW-BTC", text: "비트코인 (KRW-BTC)"},
                    {value: "KRW-ETH", text: "이더리움 (KRW-ETH)"},
                    {value: "KRW-XRP", text: "리플 (KRW-XRP)"},
                    {value: "KRW-SOL", text: "솔라나 (KRW-SOL)"},
                    {value: "KRW-ADA", text: "에이다 (KRW-ADA)"}
                ] : 
                [
                    {value: "BTCUSDT", text: "비트코인 (BTCUSDT)"},
                    {value: "ETHUSDT", text: "이더리움 (ETHUSDT)"},
                    {value: "XRPUSDT", text: "리플 (XRPUSDT)"},
                    {value: "SOLUSDT", text: "솔라나 (SOLUSDT)"},
                    {value: "ADAUSDT", text: "에이다 (ADAUSDT)"}
                ];
            
            defaultCoins.forEach(coin => {
                const option = document.createElement('option');
                option.value = coin.value;
                option.textContent = coin.text;
                select.appendChild(option);
            });
            
            // 첫 번째 코인 선택
            if (select.options.length > 1) {
                select.selectedIndex = 1;
            }
            
            // 간단한 오류 알림을 화면에 표시
            const alertDiv = document.createElement('div');
            alertDiv.className = 'alert alert-danger';
            alertDiv.innerHTML = `${exchange} 코인 목록을 불러오는 데 실패했습니다. 기본 목록을 사용합니다. <button class="btn btn-sm btn-outline-danger ms-2" onclick="location.reload()">새로고침</button>`;
            
            const cardBody = document.querySelector('.card-body');
            cardBody.insertBefore(alertDiv, cardBody.firstChild);
        });
}

/**
 * 선택한 코인 분석 실행
 * @param {string} market - 마켓 코드 (예: KRW-BTC, BTCUSDT)
 * @param {string} exchange - 거래소 코드 (upbit 또는 binance)
 */
function analyzeMarket(market, exchange) {
    // 로딩 표시
    document.getElementById('loading').style.display = 'block';
    document.getElementById('analysisResult').style.display = 'none';
    
    // 이전 WebSocket 연결 종료
    if (stompClient) {
        stompClient.disconnect();
    }
    
    fetch(`/analyze?market=${market}&exchange=${exchange}`)
        .then(response => {
            if (!response.ok) {
                throw new Error('서버 응답 오류: ' + response.status);
            }
            return response.json();
        })
        .then(data => {
            // 로딩 숨김
            document.getElementById('loading').style.display = 'none';
            
            if (data.success) {
                // 결과 표시 애니메이션
                const resultElement = document.getElementById('analysisResult');
                resultElement.classList.add('fade-in');
                resultElement.style.display = 'block';
                
                // 텍스트 분석 결과
                document.getElementById('analysisText').innerHTML = formatAnalysisText(data.analysis);
                
                // 기술적 지표
                displayTechnicalIndicators(data.indicators.latest);
                
                // 공포/욕심 지수
                displayFearGreedIndex(data.fearGreedIndex);
                
                // 뉴스 표시
                displayNews(data.news);
                
                // 차트 그리기
                drawPriceChart(data.indicators);
                drawRsiChart(data.indicators.rsi14);
                
                // 실시간 데이터 연결 (웹소켓 구현 필요)
                // stompClient = connectWebSocket(market, exchange);
                
                // 타이틀 업데이트
                updatePageTitle(market, exchange);
            } else {
                showError('분석 중 오류가 발생했습니다: ' + data.error);
            }
        })
        .catch(error => {
            document.getElementById('loading').style.display = 'none';
            console.error('분석 실패:', error);
            showError('서버 통신 중 오류가 발생했습니다: ' + error.message);
        });
}

/**
 * 분석 텍스트 포맷팅
 * @param {string} analysis - 분석 결과 텍스트
 * @return {string} 포맷팅된 HTML
 */
function formatAnalysisText(analysis) {
    // 줄바꿈과 마크다운 형식을 HTML로 변환
    return analysis
        .replace(/\n\n/g, '<br><br>')
        .replace(/\n/g, '<br>')
        .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
        .replace(/\*(.*?)\*/g, '<em>$1</em>')
        .replace(/# (.*?)$/gm, '<h3>$1</h3>')
        .replace(/## (.*?)$/gm, '<h4>$1</h4>')
        .replace(/### (.*?)$/gm, '<h5>$1</h5>');
}

/**
 * 기술적 지표 표시
 * @param {Object} indicators - 기술적 지표 객체
 */
function displayTechnicalIndicators(indicators) {
    const container = document.getElementById('technicalIndicators');
    container.innerHTML = '';
    
    if (!indicators) {
        container.innerHTML = '<div class="alert alert-warning">지표 데이터를 불러올 수 없습니다.</div>';
        return;
    }
    
    const table = document.createElement('table');
    table.className = 'table table-sm';
    
    Object.entries(indicators).forEach(([key, value]) => {
        const row = table.insertRow();
        
        const keyCell = row.insertCell();
        keyCell.textContent = formatIndicatorName(key);
        keyCell.className = 'indicator-label';
        
        const valueCell = row.insertCell();
        valueCell.textContent = parseFloat(value).toFixed(2);
        valueCell.className = 'indicator-value';
        
        // RSI에 따른 색상 지정
        if (key === 'rsi14') {
            if (value > 70) {
                valueCell.className += ' text-danger'; // 과매수
            } else if (value < 30) {
                valueCell.className += ' text-success'; // 과매도
            }
        }
    });
    
    container.appendChild(table);
}

/**
 * 지표 이름 포맷팅
 * @param {string} key - 지표 키
 * @return {string} 포맷팅된 지표 이름
 */
function formatIndicatorName(key) {
    const names = {
        'sma20': 'SMA (20)',
        'ema20': 'EMA (20)',
        'rsi14': 'RSI (14)'
    };
    
    return names[key] || key;
}

/**
 * 공포/욕심 지수 표시
 * @param {Object} data - 공포/욕심 지수 데이터
 */
function displayFearGreedIndex(data) {
    const container = document.getElementById('fearGreedIndex');
    container.innerHTML = '';
    
    if (!data) {
        container.innerHTML = '<div class="alert alert-warning">공포/욕심 지수를 불러올 수 없습니다.</div>';
        return;
    }
    
    const value = data.value;
    const classification = data.valueClassification;
    
    const valueElement = document.createElement('div');
    valueElement.className = 'fear-greed-value mb-2';
    valueElement.textContent = value;
    
    const classElement = document.createElement('div');
    classElement.className = 'mb-1';
    classElement.textContent = classification;
    
    // 지수에 따른 색상 지정
    if (value < 25) {
        valueElement.className += ' extreme-fear'; // 극도의 공포
    } else if (value < 40) {
        valueElement.className += ' fear'; // 공포
    } else if (value < 60) {
        valueElement.className += ' neutral'; // 중립
    } else if (value < 80) {
        valueElement.className += ' greed'; // 욕심
    } else {
        valueElement.className += ' extreme-greed'; // 극도의 욕심
    }
    
    container.appendChild(valueElement);
    container.appendChild(classElement);
    
    // 시각적 게이지 추가
    const gauge = document.createElement('div');
    gauge.className = 'progress';
    gauge.style.height = '20px';
    
    const bar = document.createElement('div');
    bar.className = 'progress-bar';
    bar.style.width = value + '%';
    
    // 게이지 색상
    if (value < 25) {
        bar.className += ' bg-danger';
    } else if (value < 40) {
        bar.className += ' bg-warning';
    } else if (value < 60) {
        bar.className += ' bg-info';
    } else if (value < 80) {
        bar.className += ' bg-primary';
    } else {
        bar.className += ' bg-success';
    }
    
    gauge.appendChild(bar);
    container.appendChild(gauge);
}

/**
 * 뉴스 데이터 표시
 * @param {Object} newsData - 뉴스 데이터 객체
 */
function displayNews(newsData) {
    const container = document.getElementById('newsContainer');
    container.innerHTML = '';
    
    if (!newsData || !newsData.news || newsData.news.length === 0) {
        container.innerHTML = '<p class="text-center">관련 뉴스가 없습니다.</p>';
        return;
    }
    
    const newsList = document.createElement('div');
    newsList.className = 'list-group';
    
    newsData.news.slice(0, 5).forEach(news => {
        const newsItem = document.createElement('div');
        newsItem.className = 'news-item';
        
        const title = document.createElement('div');
        title.className = 'news-title';
        title.textContent = news.title;
        
        const date = document.createElement('div');
        date.className = 'news-date';
        const publishedAt = new Date(news.published_on * 1000);
        date.textContent = publishedAt.toLocaleString();
        
        const url = document.createElement('a');
        url.href = news.url;
        url.target = '_blank';
        url.className = 'btn btn-sm btn-outline-primary mt-2';
        url.textContent = '뉴스 보기';
        
        newsItem.appendChild(title);
        newsItem.appendChild(date);
        newsItem.appendChild(url);
        
        newsList.appendChild(newsItem);
    });
    
    container.appendChild(newsList);
}

/**
 * 가격 차트 그리기
 * @param {Object} data - 차트 데이터
 */
function drawPriceChart(data) {
    const ctx = document.getElementById('priceChart').getContext('2d');
    
    // 기존 차트 제거
    if (priceChart) {
        priceChart.destroy();
    }
    
    // 데이터 확인
    if (!data || !data.sma20 || !data.ema20 || data.sma20.length === 0) {
        ctx.font = '14px Arial';
        ctx.fillText('차트 데이터를 불러올 수 없습니다.', 10, 50);
        return;
    }
    
    // 날짜 레이블 준비
    const labels = data.dates || Array.from({length: data.sma20.length}, (_, i) => i + 1);
    
    // 데이터 준비
    const sma20 = data.sma20;
    const ema20 = data.ema20;
    
    // 차트 생성
    priceChart = new Chart(ctx, {
        type: 'line',
        data: {
            labels: labels,
            datasets: [
                {
                    label: 'SMA (20)',
                    data: sma20,
                    borderColor: 'rgba(54, 162, 235, 1)',
                    backgroundColor: 'rgba(54, 162, 235, 0.1)',
                    borderWidth: 2,
                    fill: false
                },
                {
                    label: 'EMA (20)',
                    data: ema20,
                    borderColor: 'rgba(255, 99, 132, 1)',
                    backgroundColor: 'rgba(255, 99, 132, 0.1)',
                    borderWidth: 2,
                    fill: false
                }
            ]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
                x: {
                    display: true,
                    title: {
                        display: true,
                        text: '일자'
                    }
                },
                y: {
                    display: true,
                    title: {
                        display: true,
                        text: currentExchange === 'upbit' ? '가격 (KRW)' : '가격 (USDT)'
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
                    text: '가격 및 지표 추세'
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
 * @param {Array} rsiData - RSI 데이터 배열
 */
function drawRsiChart(rsiData) {
    const ctx = document.getElementById('rsiChart').getContext('2d');
    
    // 기존 차트 제거
    if (rsiChart) {
        rsiChart.destroy();
    }
    
    // 데이터 확인
    if (!rsiData || rsiData.length === 0) {
        ctx.font = '14px Arial';
        ctx.fillText('RSI 데이터를 불러올 수 없습니다.', 10, 50);
        return;
    }
    
    // 차트 생성
    rsiChart = new Chart(ctx, {
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
    
    // 추가 확장: RSI 70, 30 라인
    if (Chart.annotationLinesDrawn) {
        return;
    }
    
    // 간이 플러그인 방식으로 기준선 추가
    Chart.annotationLinesDrawn = true;
    Chart.defaults.plugins.afterDatasetsDraw = (chart) => {
        const ctx = chart.ctx;
        if (chart.canvas.id === 'rsiChart') {
            const yAxis = chart.scales.y;
            
            // 과매수 (70) 라인
            const y70 = yAxis.getPixelForValue(70);
            ctx.save();
            ctx.beginPath();
            ctx.setLineDash([5, 5]);
            ctx.moveTo(chart.chartArea.left, y70);
            ctx.lineTo(chart.chartArea.right, y70);
            ctx.lineWidth = 1;
            ctx.strokeStyle = 'rgba(255, 0, 0, 0.5)';
            ctx.stroke();
            
            // 과매도 (30) 라인
            const y30 = yAxis.getPixelForValue(30);
            ctx.beginPath();
            ctx.setLineDash([5, 5]);
            ctx.moveTo(chart.chartArea.left, y30);
            ctx.lineTo(chart.chartArea.right, y30);
            ctx.lineWidth = 1;
            ctx.strokeStyle = 'rgba(0, 255, 0, 0.5)';
            ctx.stroke();
            
            // 텍스트 추가
            ctx.font = '12px Arial';
            ctx.fillStyle = 'rgba(255, 0, 0, 0.7)';
            ctx.fillText('과매수 (70)', chart.chartArea.right - 70, y70 - 5);
            
            ctx.fillStyle = 'rgba(0, 255, 0, 0.7)';
            ctx.fillText('과매도 (30)', chart.chartArea.right - 70, y30 + 15);
            
            ctx.restore();
        }
    };
}

/**
 * WebSocket 연결 - 실시간 데이터 수신
 * @param {string} market - 마켓 코드
 * @param {string} exchange - 거래소 코드
 * @return {Object} Stomp 클라이언트
 */
function connectWebSocket(market, exchange) {
    try {
        let socket = new SockJS('/ws');
        let stompClient = Stomp.over(socket);
        
        // 로그 출력 비활성화
        stompClient.debug = null;
        
        stompClient.connect({}, function(frame) {
            console.log('WebSocket 연결 성공');
            
            // 실시간 시세 구독
            stompClient.subscribe('/topic/ticker', function(message) {
                try {
                    const tickerData = JSON.parse(message.body);
                    updateRealTimePrice(tickerData, exchange);
                } catch (e) {
                    console.error('실시간 데이터 처리 오류:', e);
                }
            });
            
            // 서버에 실시간 데이터 요청
            stompClient.send("/app/subscribe", {}, JSON.stringify({
                markets: [market],
                exchange: exchange
            }));
        }, function(error) {
            console.error('WebSocket 연결 실패:', error);
            // 오류 시 3초 후 재연결 시도
            setTimeout(() => {
                console.log('WebSocket 재연결 시도...');
                connectWebSocket(market, exchange);
            }, 3000);
        });
        
        return stompClient;
    } catch (e) {
        console.error('WebSocket 연결 중 예외 발생:', e);
        return null;
    }
}

/**
 * 실시간 가격 업데이트
 * @param {Object} data - 시세 데이터
 * @param {string} exchange - 거래소 코드
 */
function updateRealTimePrice(data, exchange) {
    if (!data || !data.code) return;
    
    const market = data.code;
    
    // 현재 선택된 마켓과 일치하는지 확인
    if (market === currentMarket) {
        // 이전 가격 저장
        const oldPrice = realTimePrices[market];
        realTimePrices[market] = data.trade_price;
        
        // 페이지 타이틀 업데이트
        updatePageTitle(market, exchange, data.trade_price, data.change_rate);
        
        // 가격 변동 시 알림 (옵션)
        if (oldPrice && Math.abs(data.change_rate) > 0.01) {
            notifyPriceChange(market, exchange, data.trade_price, data.change_rate);
        }
        
        // 실시간 가격 표시 영역이 없으면 추가
        if (!document.getElementById('realTimePrice')) {
            const priceDiv = document.createElement('div');
            priceDiv.id = 'realTimePrice';
            priceDiv.className = 'card mt-3';
            priceDiv.innerHTML = `
                <div class="card-header">실시간 가격 정보</div>
                <div class="card-body">
                    <h3 id="currentPrice">가격: 로딩 중...</h3>
                    <p id="priceChange">변동: 로딩 중...</p>
                </div>
            `;
            
            // 분석 결과 영역 앞에 추가
            const resultsDiv = document.getElementById('analysisResult');
            resultsDiv.insertBefore(priceDiv, resultsDiv.firstChild);
        }
        
        // 가격 표시 업데이트
        const currencySymbol = exchange === 'upbit' ? 'KRW' : 'USDT';
        document.getElementById('currentPrice').textContent = 
            `가격: ${data.trade_price.toLocaleString()} ${currencySymbol}`;
            
        const changePercent = (data.change_rate * 100).toFixed(2);
        const changeDirection = data.change_rate > 0 ? '▲' : (data.change_rate < 0 ? '▼' : '-');
        
        document.getElementById('priceChange').textContent = 
            `변동: ${changeDirection} ${changePercent}%`;
        document.getElementById('priceChange').className = 
            data.change_rate > 0 ? 'text-success' : 
            (data.change_rate < 0 ? 'text-danger' : '');
    }
}

/**
 * 페이지 타이틀 업데이트
 * @param {string} market - 마켓 코드
 * @param {string} exchange - 거래소 코드
 * @param {number} price - 현재 가격
 * @param {number} changeRate - 변동률
 */
function updatePageTitle(market, exchange, price, changeRate) {
    const marketSelect = document.getElementById('marketSelect');
    if (!marketSelect) return;
    
    const selectedOption = marketSelect.options[marketSelect.selectedIndex];
    if (!selectedOption) return;
    
    const marketName = selectedOption.text;
    const currencySymbol = exchange === 'upbit' ? 'KRW' : 'USDT';
    
    if (price) {
        const direction = changeRate > 0 ? '▲' : (changeRate < 0 ? '▼' : '-');
        const changePercent = Math.abs(changeRate * 100).toFixed(2);
        document.title = `${price.toLocaleString()} ${currencySymbol} ${direction} ${changePercent}% | ${marketName}`;
    } else {
        document.title = `${marketName} - 가상화폐 AI 분석`;
    }
}

/**
 * 가격 변동 알림
 * @param {string} market - 마켓 코드
 * @param {string} exchange - 거래소 코드
 * @param {number} price - 현재 가격
 * @param {number} changeRate - 변동률
 */
function notifyPriceChange(market, exchange, price, changeRate) {
    const marketSelect = document.getElementById('marketSelect');
    if (!marketSelect) return;
    
    const selectedOption = marketSelect.options[marketSelect.selectedIndex];
    if (!selectedOption) return;
    
    const marketName = selectedOption.text;
    const direction = changeRate > 0 ? '상승' : '하락';
    const changePercent = (Math.abs(changeRate) * 100).toFixed(2);
    const currencySymbol = exchange === 'upbit' ? 'KRW' : 'USDT';
    
    if ('Notification' in window && Notification.permission === 'granted') {
        new Notification(`${marketName} ${direction} 알림`, {
            body: `${changePercent}% ${direction}했습니다. 현재가: ${price.toLocaleString()} ${currencySymbol}`,
            icon: '/favicon.ico'
        });
    }
}

/**
 * 알림 권한 요청
 */
function requestNotificationPermission() {
    if ('Notification' in window) {
        if (Notification.permission !== 'granted' && Notification.permission !== 'denied') {
            Notification.requestPermission();
        }
    }
}

/**
 * 오류 메시지 표시
 * @param {string} message - 오류 메시지
 */
function showError(message) {
    // 모달 알림 대신 인라인 알림으로 변경
    const alertDiv = document.createElement('div');
    alertDiv.className = 'alert alert-danger mt-3';
    alertDiv.innerHTML = message + ' <button type="button" class="btn-close float-end" data-bs-dismiss="alert" aria-label="Close"></button>';
    
    // 알림 추가
    const container = document.querySelector('.container');
    container.insertBefore(alertDiv, document.getElementById('loading'));
    
    // 5초 후 자동 제거
    setTimeout(() => {
        alertDiv.classList.add('fade');
        setTimeout(() => alertDiv.remove(), 500);
    }, 5000);
}